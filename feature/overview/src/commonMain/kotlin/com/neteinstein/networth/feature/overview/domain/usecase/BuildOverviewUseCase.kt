package com.neteinstein.networth.feature.overview.domain.usecase

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.averageMonthlySavings
import com.neteinstein.networth.core.domain.calc.balanceOf
import com.neteinstein.networth.core.domain.calc.coveredMonths
import com.neteinstein.networth.core.domain.calc.isChildAccount
import com.neteinstein.networth.core.domain.calc.monthlySpending
import com.neteinstein.networth.core.domain.calc.netWorth
import com.neteinstein.networth.core.domain.calc.projectSavings
import com.neteinstein.networth.core.domain.calc.savingsBalance
import com.neteinstein.networth.core.domain.calc.scopeChoices
import com.neteinstein.networth.core.domain.calc.spikes
import com.neteinstein.networth.core.domain.calc.totalsOf
import com.neteinstein.networth.core.domain.calc.trailingAverage
import com.neteinstein.networth.core.domain.calc.transactionsIn
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.feature.overview.domain.model.OverviewPeriod
import com.neteinstein.networth.feature.overview.domain.model.TrendStep
import kotlinx.datetime.LocalDate

/** One slice of "Where it went". A null [categoryId] is spending not yet categorised. */
data class CategoryShare(
    val categoryId: String?,
    val name: String?,
    val colorHex: String?,
    val amountMinor: Long,
    val percent: Int,
)

/** One bar of the trend: the [step] starting at [month] (a month, a quarter or a year). */
data class TrendPoint(
    val month: CalendarMonth,
    val spentMinor: Long,
    val isSelected: Boolean,
    val step: TrendStep = TrendStep.MONTH,
)

data class AccountLine(
    val id: String,
    val name: String,
    val type: AccountType,
    val balanceMinor: Long,
    val isChild: Boolean,
)

data class SpikeSummary(
    val categoryId: String,
    val categoryName: String,
    val spentMinor: Long,
    val averageMinor: Long,
    val ratio: Double,
)

/** Everything the Overview artboard shows, for one period (a month or an aggregate) and one account scope. */
data class Overview(
    val householdName: String,
    val adults: Int,
    val children: Int,
    val currencyCode: String,
    val period: OverviewPeriod,
    /** The first and last day the figures cover. */
    val from: LocalDate,
    val to: LocalDate,
    /** The month of [to]: the selected month when [period] is a month. */
    val month: CalendarMonth,
    /** Months that can be picked: every month with data, plus a selected month. Newest first. */
    val months: List<CalendarMonth>,
    /** Years that can be picked: every year with data, plus a selected year. Newest first. */
    val years: List<Int>,
    val scopes: List<AccountScope>,
    val scope: AccountScope,
    val hasTransactions: Boolean,
    /** Uncategorised outflows waiting in Review & learn. */
    val needsReview: Int,
    val incomeMinor: Long,
    val spentMinor: Long,
    val savedMinor: Long,
    val savedPercent: Int?,
    val spike: SpikeSummary?,
    val shares: List<CategoryShare>,
    val trend: List<TrendPoint>,
    /** A month: the 12-month average before it. An aggregate: the average spent per month with data in it. */
    val averageSpentMinor: Long,
    val netWorthMinor: Long,
    val accounts: List<AccountLine>,
    val projectedSavingsMinor: Long?,
) {
    val topShares: List<CategoryShare> get() = shares.take(TOP_CATEGORIES)
    val otherShares: List<CategoryShare> get() = shares.drop(TOP_CATEGORIES)

    companion object {
        const val TOP_CATEGORIES = 5
        const val TREND_MONTHS = 6

        /** The most bars an aggregate's trend shows; longer periods group by quarter, then by year. */
        const val TREND_BARS = 6
        const val PROJECTION_YEARS = 5
    }
}

class BuildOverviewUseCase {
    /** [today] only matters for [OverviewPeriod.AllTime] without any movements. */
    operator fun invoke(database: FamilyDatabase, period: OverviewPeriod, scope: AccountScope, today: LocalDate): Overview {
        val scopes = database.scopeChoices()
        val effectiveScope = scope.takeIf { it in scopes } ?: AccountScope.All
        val (from, to) = database.rangeOf(period, effectiveScope, today)
        val month = CalendarMonth.of(to)
        val totals = totalsOf(database.transactionsIn(effectiveScope, from, to))
        val spent = totals.spentMinor
        val shares = totals.spentByCategory.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { (categoryId, amount) ->
                val category = database.category(categoryId)
                CategoryShare(categoryId, category?.name, category?.colorHex, amount, if (spent > 0) (amount * 100 / spent).toInt() else 0)
            }
        val covered = database.coveredMonths()
        // Spikes compare one month with its own average, so only a single month has one.
        val spike = if (!period.isMonth) {
            null
        } else {
            database.spikes(month, effectiveScope).firstOrNull()?.let { s ->
                SpikeSummary(s.categoryId, database.category(s.categoryId)?.name.orEmpty(), s.spentMinor, s.averageMinor, s.ratioToAverage)
            }
        }
        val netWorth = database.netWorth()
        val monthlySavings = database.averageMonthlySavings(month)
        return Overview(
            householdName = database.settings.householdName,
            adults = database.members.count { it.role == MemberRole.ADULT },
            children = database.members.count { it.role == MemberRole.CHILD },
            currencyCode = database.settings.currencyCode,
            period = period,
            from = from,
            to = to,
            month = month,
            months = (covered + listOfNotNull((period as? OverviewPeriod.Month)?.month)).sortedDescending(),
            years = (covered.map { it.year } + listOfNotNull((period as? OverviewPeriod.Year)?.year)).distinct().sortedDescending(),
            scopes = scopes,
            scope = effectiveScope,
            hasTransactions = database.transactions.isNotEmpty(),
            needsReview = database.transactions.count { it.needsReview },
            incomeMinor = totals.incomeMinor,
            spentMinor = spent,
            savedMinor = totals.savedMinor,
            savedPercent = totals.savedPercent,
            spike = spike,
            shares = shares,
            trend = if (period.isMonth) {
                database.monthlySpending(effectiveScope, month, Overview.TREND_MONTHS).map { (m, value) ->
                    TrendPoint(m, value, m == month)
                }
            } else {
                database.trend(effectiveScope, from, to)
            },
            averageSpentMinor = if (period.isMonth) {
                database.trailingAverage(effectiveScope, month).spentMinor
            } else {
                val monthsWithData = database.coveredMonths(effectiveScope).count { it >= CalendarMonth.of(from) && it <= month }
                if (monthsWithData > 0) spent / monthsWithData else 0L
            },
            netWorthMinor = netWorth.netMinor,
            accounts = database.accounts.map { account ->
                AccountLine(account.id, account.name, account.type, database.balanceOf(account), database.isChildAccount(account))
            },
            projectedSavingsMinor = monthlySavings.takeIf { it > 0 && covered.isNotEmpty() }?.let {
                projectSavings(database.savingsBalance(), it, Overview.PROJECTION_YEARS).last()
            },
        )
    }

    private fun FamilyDatabase.rangeOf(period: OverviewPeriod, scope: AccountScope, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (period) {
            is OverviewPeriod.Month -> period.month.firstDay to period.month.lastDay
            is OverviewPeriod.Year -> LocalDate(period.year, 1, 1) to LocalDate(period.year, 12, 31)
            is OverviewPeriod.Custom -> period.start to period.end
            OverviewPeriod.AllTime -> {
                val ids = scope.accountIds(this)
                val dates = transactions.filter { it.accountId in ids }.map { it.date }
                (dates.minOrNull() ?: today) to (dates.maxOrNull() ?: today)
            }
        }

    /** Spending per calendar month, quarter or year across [from]–[to]: the finest step that fits in [Overview.TREND_BARS] bars. */
    private fun FamilyDatabase.trend(scope: AccountScope, from: LocalDate, to: LocalDate): List<TrendPoint> {
        val first = CalendarMonth.of(from)
        val last = CalendarMonth.of(to)
        val step = TrendStep.entries.firstOrNull { bucketStarts(it, first, last).size <= Overview.TREND_BARS } ?: TrendStep.YEAR
        return bucketStarts(step, first, last).takeLast(Overview.TREND_BARS).map { start ->
            val end = start.plus(step.months - 1).lastDay
            val spent = totalsOf(transactionsIn(scope, maxOf(start.firstDay, from), minOf(end, to))).spentMinor
            TrendPoint(start, spent, isSelected = false, step = step)
        }
    }

    private fun bucketStarts(step: TrendStep, first: CalendarMonth, last: CalendarMonth): List<CalendarMonth> {
        val aligned = CalendarMonth(first.year, (first.month - 1) / step.months * step.months + 1)
        return generateSequence(aligned) { it.plus(step.months) }.takeWhile { it <= last }.toList()
    }
}
