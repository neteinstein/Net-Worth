package com.neteinstein.networth.feature.alerts.domain.usecase

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.coveredMonths
import com.neteinstein.networth.core.domain.calc.isChildAccount
import com.neteinstein.networth.core.domain.calc.latestMonth
import com.neteinstein.networth.core.domain.calc.seasonalCategories
import com.neteinstein.networth.core.domain.calc.spikes
import com.neteinstein.networth.core.domain.calc.totalsOf
import com.neteinstein.networth.core.domain.calc.trailingAverage
import com.neteinstein.networth.core.domain.calc.transactionsIn
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.text.Descriptions
import com.neteinstein.networth.core.domain.text.MovementKind
import com.neteinstein.networth.feature.alerts.domain.model.AlertsPeriod
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/** An account as a sentence names it: "Pedro’s account" when it is named after its owner, else its name. */
data class AccountName(
    val name: String,
    val isPersonal: Boolean,
)

/** Where a spike's money went: "mostly Pedro's account", "both current accounts", or spread out. */
sealed interface AccountShare {
    data class Mostly(
        val account: AccountName,
    ) : AccountShare

    data object BothCurrent : AccountShare

    data class Spread(
        val accounts: Int,
    ) : AccountShare
}

data class SpikeAlert(
    val categoryId: String,
    val name: String,
    val colorHex: String,
    val month: CalendarMonth,
    val spentMinor: Long,
    val averageMinor: Long,
    val percentAboveAverage: Int,
    val accounts: AccountShare?,
    /** Spending per subcategory ("Books & materials €385"); null subcategory name means "Other". */
    val parts: List<Pair<String?, Long>>,
    val sameMonthLastYearMinor: Long?,
    /** Last year's same month was a spike too: "this repeats every year". */
    val repeatsEveryYear: Boolean,
    val purchases: Int,
    val firstDay: LocalDate?,
    val lastDay: LocalDate?,
    /** Already marked as seasonal (planned) for this month. */
    val seasonal: Boolean,
)

/** "Worth a look" cards. */
sealed interface Insight {
    /** A category past its budget: "Groceries €1,040 of a €900 budget" (the monthly budget × [months] for a longer period). */
    data class OverBudget(
        val name: String,
        val spentMinor: Long,
        val budgetMinor: Long,
        val months: Int = 1,
    ) : Insight {
        val overMinor: Long get() = spentMinor - budgetMinor
    }

    data class NewRecurring(
        val name: String,
        val amountMinor: Long,
        val since: LocalDate,
        val account: AccountName,
    ) : Insight {
        val yearlyMinor: Long get() = amountMinor * 12
    }

    /** Spending up three months running, e.g. Fuel €212 → €226 → €238 (Jul–Sep). */
    data class Rising(
        val name: String,
        val values: List<Long>,
        val first: CalendarMonth,
        val last: CalendarMonth,
    ) : Insight

    data class LargePayment(
        val description: String,
        val amountMinor: Long,
        val date: LocalDate,
        val accountName: String,
    ) : Insight
}

/** "Going well" lines. */
sealed interface GoodNews {
    /** A category clearly under its average: "Dining & leisure ▼ −21% · €60 under avg". */
    data class UnderAverage(
        val name: String,
        val percentBelow: Int,
        val underMinor: Long,
    ) : GoodNews

    /** Every kids' savings account received money every month: "+€30 each, 9 months in a row". */
    data class SavingsStreak(
        val accounts: Int,
        /** Set when every account received the same amount this month. */
        val amountEachMinor: Long?,
        val months: Int,
    ) : GoodNews
}

data class Alerts(
    val currencyCode: String,
    /** The period the alerts are about (by default the latest month with data), or null without any history. */
    val period: AlertsPeriod?,
    val thresholdPct: Int,
    /** Newest month first. */
    val spikes: List<SpikeAlert>,
    val worthALook: List<Insight>,
    val goingWell: List<GoodNews>,
    /** Months that can be picked: every month with data, plus a selected month. Newest first. */
    val months: List<CalendarMonth> = emptyList(),
    /** Years that can be picked: every year with data, plus a selected year. Newest first. */
    val years: List<Int> = emptyList(),
    /** The first and last day the period covers. */
    val from: LocalDate? = null,
    val to: LocalDate? = null,
)

/**
 * The Alerts artboard, from the family's own history: spikes against the 12-month average (with
 * the subcategories and accounts behind them), new recurring payments, categories rising three
 * months running, single payments over the large-payment limit, and what went well.
 *
 * A longer [AlertsPeriod] lists the spikes of every month in it; budgets, new recurring payments,
 * large payments and averages cover the whole period, while rises and the savings streak end with
 * its last month with data.
 */
class BuildAlertsUseCase {
    /** A null [period] follows the latest month with data. */
    operator fun invoke(database: FamilyDatabase, period: AlertsPeriod? = null): Alerts {
        val settings = database.settings.alerts
        val latest =
            database.latestMonth()
                ?: return Alerts(database.settings.currencyCode, null, settings.spikeThresholdPct, emptyList(), emptyList(), emptyList())
        val effective = period ?: AlertsPeriod.Month(latest)
        val (from, to) = database.rangeOf(effective)
        val covered = database.coveredMonths(AccountScope.All)
        // Months of the period that have data, newest first.
        val months = covered.filter { it >= CalendarMonth.of(from) && it <= CalendarMonth.of(to) }.sortedDescending()
        val base =
            Alerts(
                currencyCode = database.settings.currencyCode,
                period = effective,
                thresholdPct = settings.spikeThresholdPct,
                spikes = emptyList(),
                worthALook = emptyList(),
                goingWell = emptyList(),
                months = (covered + listOfNotNull((effective as? AlertsPeriod.Month)?.month)).sortedDescending(),
                years = (covered.map { it.year } + listOfNotNull((effective as? AlertsPeriod.Year)?.year)).distinct().sortedDescending(),
                from = from,
                to = to,
            )
        val last = months.firstOrNull() ?: return base
        val spikes = months.flatMap { spikeAlerts(database, it) }
        val lastSpikeIds = spikes.filter { it.month == last }.map { it.categoryId }.toSet()
        // A spike already has its own card; over a longer period the budget is about the whole of it.
        val budgetSkip = if (effective is AlertsPeriod.Month) lastSpikeIds else emptySet()
        val worth =
            buildList {
                addAll(overBudget(database, from, to, months.size, budgetSkip))
                if (settings.newSubscriptions) addAll(months.flatMap { newRecurring(database, it) }.distinctBy { it.name })
                addAll(rising(database, last, lastSpikeIds))
                addAll(largePayments(database, from, to))
            }
        val good = underAverage(database, from, to, months) + listOfNotNull(savingsStreak(database, last))
        return base.copy(spikes = spikes, worthALook = worth, goingWell = good)
    }

    private fun FamilyDatabase.rangeOf(period: AlertsPeriod): Pair<LocalDate, LocalDate> = when (period) {
        is AlertsPeriod.Month -> period.month.firstDay to period.month.lastDay
        is AlertsPeriod.Year -> LocalDate(period.year, 1, 1) to LocalDate(period.year, 12, 31)
        is AlertsPeriod.Custom -> period.start to period.end
        AlertsPeriod.AllTime -> transactions.minOf { it.date } to transactions.maxOf { it.date }
    }

    private fun accountName(
        database: FamilyDatabase,
        accountId: String,
    ): AccountName? = database.account(accountId)?.let { account ->
        AccountName(account.name, database.members.any { it.name == account.name })
    }

    private fun outflows(
        database: FamilyDatabase,
        month: CalendarMonth,
    ): List<Transaction> = outflows(database, month.firstDay, month.lastDay)

    private fun outflows(
        database: FamilyDatabase,
        from: LocalDate,
        to: LocalDate,
    ): List<Transaction> =
        database.transactions.filter { it.date >= from && it.date <= to && it.amountMinor < 0 && !it.isInternalTransfer }

    private fun spikeAlerts(
        database: FamilyDatabase,
        month: CalendarMonth,
    ): List<SpikeAlert> {
        val threshold = database.settings.alerts.spikeThresholdPct
        val seasonal = database.seasonalCategories(month)
        val spending = outflows(database, month)
        return database.spikes(month, includeSeasonal = true).mapNotNull { spike ->
            val category = database.category(spike.categoryId) ?: return@mapNotNull null
            val txs = spending.filter { it.categoryId == category.id }
            val byAccount = txs.groupBy { it.accountId }.mapValues { (_, list) -> -list.sumOf { it.amountMinor } }
            val total = byAccount.values.sum()
            val top = byAccount.maxByOrNull { it.value }
            val accounts =
                when {
                    top == null || total <= 0 -> null
                    top.value * 100 >= total * MOSTLY_PERCENT -> accountName(database, top.key)?.let { AccountShare.Mostly(it) }
                    byAccount.size == 2 &&
                        byAccount.keys.all {
                            database.account(it)?.type == AccountType.CURRENT
                        } -> AccountShare.BothCurrent
                    else -> AccountShare.Spread(byAccount.size)
                }
            val parts =
                txs.groupBy { it.subcategoryId }
                    .map { (sub, list) -> category.subcategories.firstOrNull { it.id == sub }?.name to -list.sumOf { it.amountMinor } }
                    .sortedByDescending { it.second }
                    .takeIf { list -> list.any { it.first != null } }
                    .orEmpty()
            val lastYear = spike.sameMonthLastYearMinor
            SpikeAlert(
                categoryId = category.id,
                name = category.name,
                colorHex = category.colorHex,
                month = month,
                spentMinor = spike.spentMinor,
                averageMinor = spike.averageMinor,
                percentAboveAverage = spike.percentAboveAverage,
                accounts = accounts,
                parts = parts,
                sameMonthLastYearMinor = lastYear,
                repeatsEveryYear = lastYear != null && lastYear * 100 >= spike.averageMinor * (100 + threshold),
                purchases = txs.size,
                firstDay = txs.minOfOrNull { it.date },
                lastDay = txs.maxOfOrNull { it.date },
                seasonal = category.id in seasonal,
            )
        }
    }

    /**
     * A merchant charged in this month and the one before, for about the same amount (±10 %), and
     * never before — with at least one older month of history to compare against.
     */
    private fun newRecurring(
        database: FamilyDatabase,
        month: CalendarMonth,
    ): List<Insight.NewRecurring> {
        val previous = month.previous()
        if (month.plus(-2) !in database.coveredMonths(AccountScope.All)) return emptyList()
        val keyed =
            database.transactions.filter { it.amountMinor < 0 && !it.isInternalTransfer }.mapNotNull { tx ->
                val info = Descriptions.describe(tx.description)
                if (info.kind in NOT_SUBSCRIPTIONS) return@mapNotNull null
                val key = info.key ?: return@mapNotNull null
                Triple(key, info.merchant ?: info.counterparty ?: key, tx)
            }.groupBy { it.first }
        return keyed.mapNotNull { (_, entries) ->
            val txs = entries.map { it.third }
            if (txs.any { CalendarMonth.of(it.date) < previous }) return@mapNotNull null
            val now = txs.filter { it.date in month }
            val before = txs.filter { it.date in previous }
            if (now.size != 1 || before.size != 1) return@mapNotNull null
            val a = abs(now.single().amountMinor)
            val b = abs(before.single().amountMinor)
            if (abs(a - b) * 100 > maxOf(a, b) * SAME_AMOUNT_TOLERANCE_PERCENT) return@mapNotNull null
            val first = before.single()
            Insight.NewRecurring(entries.first().second, a, first.date, accountName(database, first.accountId) ?: AccountName("", false))
        }.sortedByDescending { it.amountMinor }
    }

    /** Subcategories (or categories without them) whose spending rose in each of the last three months. */
    private fun rising(
        database: FamilyDatabase,
        month: CalendarMonth,
        skip: Set<String>,
    ): List<Insight.Rising> {
        val months = listOf(month.plus(-2), month.previous(), month)
        val spending = months.associateWith { outflows(database, it) }

        fun series(select: (Transaction) -> Boolean) = months.map { m -> -spending.getValue(m).filter(select).sumOf { it.amountMinor } }

        fun risingSeries(values: List<Long>) = values.all { it > 0 } && values.zipWithNext().all { (x, y) -> y > x }
        val result = ArrayList<Pair<Insight.Rising, Double>>()
        database.categories.filter { it.id !in skip }.forEach { category ->
            val subs =
                category.subcategories.mapNotNull { sub ->
                    val values = series { it.categoryId == category.id && it.subcategoryId == sub.id }
                    if (risingSeries(values)) {
                        Insight.Rising(sub.name, values, months.first(), month) to
                            values.last().toDouble() / values.first()
                    } else {
                        null
                    }
                }
            if (subs.isNotEmpty()) {
                result += subs
            } else {
                val values = series { it.categoryId == category.id }
                if (risingSeries(values)) {
                    result +=
                        Insight.Rising(category.name, values, months.first(), month) to values.last().toDouble() / values.first()
                }
            }
        }
        return result.sortedByDescending { it.second }.take(MAX_RISING).map { it.first }
    }

    private fun largePayments(
        database: FamilyDatabase,
        from: LocalDate,
        to: LocalDate,
    ): List<Insight.LargePayment> {
        val limit = database.settings.alerts.largePaymentMinor
        if (limit <= 0) return emptyList()
        return outflows(database, from, to)
            .filter { -it.amountMinor >= limit && Descriptions.describe(it.description).kind != MovementKind.MORTGAGE }
            .sortedBy { it.amountMinor }
            .take(MAX_LARGE)
            .map { Insight.LargePayment(it.description, -it.amountMinor, it.date, database.account(it.accountId)?.name.orEmpty()) }
    }

    /** Categories over their budget for the [months] with data, largest overrun first; [skip] already have their own card. */
    private fun overBudget(
        database: FamilyDatabase,
        from: LocalDate,
        to: LocalDate,
        months: Int,
        skip: Set<String>,
    ): List<Insight.OverBudget> {
        val spent = totalsOf(database.transactionsIn(AccountScope.All, from, to)).spentByCategory
        return database.categories
            .filter { it.id !in skip }
            .mapNotNull { category ->
                val budget = category.monthlyBudgetMinor?.takeIf { it > 0 }?.times(months) ?: return@mapNotNull null
                val amount = spent[category.id] ?: return@mapNotNull null
                if (amount > budget) Insight.OverBudget(category.name, amount, budget, months) else null
            }.sortedByDescending { it.overMinor }
    }

    /** Monthly spending over the period's [months] with data, against the 12 months before its first. */
    private fun underAverage(
        database: FamilyDatabase,
        from: LocalDate,
        to: LocalDate,
        months: List<CalendarMonth>,
    ): List<GoodNews.UnderAverage> {
        val current = totalsOf(database.transactionsIn(AccountScope.All, from, to)).spentByCategory
        val average = database.trailingAverage(AccountScope.All, months.min()).spentByCategory
        return database.categories.mapNotNull { category ->
            val avg = average[category.id]?.takeIf { it >= MIN_AVERAGE_MINOR } ?: return@mapNotNull null
            val spent = ((current[category.id] ?: 0L) / months.size).coerceAtLeast(0)
            val below = ((avg - spent) * 100 / avg).toInt()
            if (below < UNDER_AVERAGE_PERCENT) null else GoodNews.UnderAverage(category.name, below, avg - spent)
        }.sortedByDescending { it.underMinor }.take(MAX_UNDER)
    }

    /** Months in a row, ending with [month], in which every kids' account received money. */
    private fun savingsStreak(
        database: FamilyDatabase,
        month: CalendarMonth,
    ): GoodNews.SavingsStreak? {
        val kids = database.accounts.filter { database.isChildAccount(it) }
        if (kids.isEmpty()) return null

        fun received(
            accountId: String,
            m: CalendarMonth,
        ) = database.transactions.filter { it.accountId == accountId && it.date in m && it.amountMinor > 0 }.sumOf { it.amountMinor }
        var streak = 0
        while (streak < MAX_STREAK_MONTHS && kids.all { received(it.id, month.plus(-streak)) > 0 }) streak++
        if (streak < MIN_STREAK) return null
        val amounts = kids.map { received(it.id, month) }.distinct()
        return GoodNews.SavingsStreak(kids.size, amounts.singleOrNull(), streak)
    }

    private companion object {
        const val MOSTLY_PERCENT = 70
        const val SAME_AMOUNT_TOLERANCE_PERCENT = 10
        const val MAX_RISING = 2
        const val MAX_LARGE = 3
        const val MAX_UNDER = 3
        const val UNDER_AVERAGE_PERCENT = 20
        const val MIN_AVERAGE_MINOR = 50_00L
        const val MIN_STREAK = 3
        const val MAX_STREAK_MONTHS = 120
        val NOT_SUBSCRIPTIONS =
            setOf(
                MovementKind.LOW_VALUE_CARD,
                MovementKind.CASH_WITHDRAWAL,
                MovementKind.FEE,
                MovementKind.TAX,
                MovementKind.MORTGAGE,
                MovementKind.TRANSFER_IN,
                MovementKind.REFUND,
            )
    }
}
