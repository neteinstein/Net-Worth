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
import com.neteinstein.networth.core.domain.calc.totals
import com.neteinstein.networth.core.domain.calc.trailingAverage
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MemberRole

/** One slice of "Where it went". A null [categoryId] is spending not yet categorised. */
data class CategoryShare(
    val categoryId: String?,
    val name: String?,
    val colorHex: String?,
    val amountMinor: Long,
    val percent: Int,
)

data class TrendPoint(
    val month: CalendarMonth,
    val spentMinor: Long,
    val isSelected: Boolean,
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

/** Everything the Overview artboard shows, for one month and one account scope. */
data class Overview(
    val householdName: String,
    val adults: Int,
    val children: Int,
    val currencyCode: String,
    val month: CalendarMonth,
    /** Months that can be picked: every month with data, plus [month]. Newest first. */
    val months: List<CalendarMonth>,
    val scopes: List<AccountScope>,
    val scope: AccountScope,
    val hasTransactions: Boolean,
    val incomeMinor: Long,
    val spentMinor: Long,
    val savedMinor: Long,
    val savedPercent: Int?,
    val spike: SpikeSummary?,
    val shares: List<CategoryShare>,
    val trend: List<TrendPoint>,
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
        const val PROJECTION_YEARS = 5
    }
}

class BuildOverviewUseCase {
    operator fun invoke(database: FamilyDatabase, month: CalendarMonth, scope: AccountScope): Overview {
        val scopes = database.scopeChoices()
        val effectiveScope = scope.takeIf { it in scopes } ?: AccountScope.All
        val totals = database.totals(effectiveScope, month)
        val spent = totals.spentMinor
        val shares = totals.spentByCategory.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { (categoryId, amount) ->
                val category = database.category(categoryId)
                CategoryShare(categoryId, category?.name, category?.colorHex, amount, if (spent > 0) (amount * 100 / spent).toInt() else 0)
            }
        val covered = database.coveredMonths()
        val spike = database.spikes(month, effectiveScope).firstOrNull()?.let { s ->
            SpikeSummary(s.categoryId, database.category(s.categoryId)?.name.orEmpty(), s.spentMinor, s.averageMinor, s.ratioToAverage)
        }
        val netWorth = database.netWorth()
        val monthlySavings = database.averageMonthlySavings(month)
        return Overview(
            householdName = database.settings.householdName,
            adults = database.members.count { it.role == MemberRole.ADULT },
            children = database.members.count { it.role == MemberRole.CHILD },
            currencyCode = database.settings.currencyCode,
            month = month,
            months = (covered + month).sortedDescending(),
            scopes = scopes,
            scope = effectiveScope,
            hasTransactions = database.transactions.isNotEmpty(),
            incomeMinor = totals.incomeMinor,
            spentMinor = spent,
            savedMinor = totals.savedMinor,
            savedPercent = totals.savedPercent,
            spike = spike,
            shares = shares,
            trend = database.monthlySpending(effectiveScope, month, Overview.TREND_MONTHS).map { (m, value) ->
                TrendPoint(
                    m,
                    value,
                    m == month,
                )
            },
            averageSpentMinor = database.trailingAverage(effectiveScope, month).spentMinor,
            netWorthMinor = netWorth.netMinor,
            accounts = database.accounts.map { account ->
                AccountLine(account.id, account.name, account.type, database.balanceOf(account), database.isChildAccount(account))
            },
            projectedSavingsMinor = monthlySavings.takeIf { it > 0 && covered.isNotEmpty() }?.let {
                projectSavings(database.savingsBalance(), it, Overview.PROJECTION_YEARS).last()
            },
        )
    }
}
