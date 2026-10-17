package com.neteinstein.networth.feature.spending.domain.usecase

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.isSavings
import com.neteinstein.networth.core.domain.calc.scopeChoices
import com.neteinstein.networth.core.domain.calc.totalsOf
import com.neteinstein.networth.core.domain.calc.trailingAverage
import com.neteinstein.networth.core.domain.calc.transactionsIn
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.feature.spending.domain.model.DateRange
import com.neteinstein.networth.feature.spending.domain.model.PeriodKind

/** One bar of "Category vs 12-month avg". [categoryId] null is "Not categorised yet". */
data class CategoryRow(
    val categoryId: String?,
    val name: String?,
    val colorHex: String?,
    val amountMinor: Long,
    /** The 12-month average scaled to the period's length, or null without history. */
    val averageMinor: Long?,
    val isSpike: Boolean,
) {
    /** Change against the average in percent (+46 means 46 % above), or null without history. */
    val deltaPercent: Int?
        get() = averageMinor?.takeIf { it > 0 }?.let { ((amountMinor - it) * 100 / it).toInt() }
}

/** Money in and out of savings accounts, which have no spending of their own. */
data class SavingsFlow(
    val inMinor: Long,
    val outMinor: Long,
)

data class Spending(
    val currencyCode: String,
    val kind: PeriodKind,
    val period: DateRange,
    val scopes: List<AccountScope>,
    val scope: AccountScope,
    val scopeName: String?,
    /** Names of the single-account chips. */
    val scopeNames: Map<String, String>,
    /** The accounts in [scope], or empty for all accounts: what a tapped row's transaction list shows. */
    val scopeAccountIds: List<String>,
    val totalMinor: Long,
    /** All accounts: spending divided by the household's size. */
    val perPersonMinor: Long?,
    /** One account: its share of the family's spending, in percent. */
    val shareOfFamilyPercent: Int?,
    val rows: List<CategoryRow>,
    /** Set when the scope is savings only (joint or kids' savings): no spending, just money in and out. */
    val savings: SavingsFlow?,
    val spikeThresholdPct: Int,
) {
    val maxRowMinor: Long get() = rows.maxOfOrNull { it.amountMinor } ?: 0L
}

/**
 * The Spending artboard: what the selected accounts spent per category in a period, against the
 * 12-month average scaled to the period (a month as is, a year is 12 months, a week or custom range by its days).
 * Internal transfers are never spending (see `PeriodTotals`).
 */
class BuildSpendingUseCase {
    operator fun invoke(
        database: FamilyDatabase,
        kind: PeriodKind,
        period: DateRange,
        requestedScope: AccountScope,
    ): Spending {
        val scopes = database.scopeChoices()
        val scope = requestedScope.takeIf { it in scopes } ?: AccountScope.All
        val ids = scope.accountIds(database)
        val inScope = database.accounts.filter { it.id in ids }
        val isSavingsScope = inScope.isNotEmpty() && inScope.all { it.type.isSavings } && scope != AccountScope.All
        val movements = database.transactionsIn(scope, period.start, period.end)
        val totals = totalsOf(movements)
        val familyTotal = totalsOf(database.transactionsIn(AccountScope.All, period.start, period.end)).spentMinor

        // The 12 months before the period, like Overview, so this period never dilutes its own baseline.
        val monthlyAverage = database.trailingAverage(scope, CalendarMonth.of(period.start)).spentByCategory
        val scale =
            when (kind) {
                PeriodKind.MONTH -> 1.0
                PeriodKind.YEAR -> MONTHS_PER_YEAR
                PeriodKind.WEEK, PeriodKind.CUSTOM -> period.days / DAYS_PER_MONTH
            }
        val threshold = database.settings.alerts.spikeThresholdPct
        val categories = database.categories.sortedBy { it.position }
        val rows =
            if (isSavingsScope) {
                emptyList()
            } else {
                val keys = (totals.spentByCategory.keys + if (scope == AccountScope.All) monthlyAverage.keys else emptySet()).distinct()
                keys.mapNotNull { id ->
                    val category = id?.let { categoryId -> categories.firstOrNull { it.id == categoryId } }
                    if (id != null && category == null) return@mapNotNull null
                    val amount = totals.spentByCategory[id] ?: 0L
                    val average = monthlyAverage[id]?.takeIf { it > 0 }?.let { (it * scale).toLong() }
                    val spike =
                        category?.warnOnSpike == true &&
                            average != null &&
                            average > 0 &&
                            (amount - average) * 100 >= threshold.toLong() * average
                    CategoryRow(id, category?.name, category?.colorHex, amount, average, spike)
                }.filter { it.amountMinor > 0 || (it.averageMinor ?: 0) > 0 }
                    .sortedWith(compareByDescending<CategoryRow> { it.amountMinor }.thenByDescending { it.averageMinor ?: 0 })
            }
        val savings =
            if (isSavingsScope) {
                val counted = movements.filter { it.accountId in ids }
                SavingsFlow(
                    counted.filter {
                        it.amountMinor > 0
                    }.sumOf { it.amountMinor },
                    -counted.filter { it.amountMinor < 0 }.sumOf { it.amountMinor },
                )
            } else {
                null
            }
        val people = database.members.size.coerceAtLeast(1)
        return Spending(
            currencyCode = database.settings.currencyCode,
            kind = kind,
            period = period,
            scopes = scopes,
            scope = scope,
            scopeName = (scope as? AccountScope.Single)?.let { database.account(it.accountId)?.name },
            scopeNames = database.accounts.associate { it.id to it.name },
            scopeAccountIds = if (scope == AccountScope.All) emptyList() else ids.toList(),
            totalMinor = totals.spentMinor,
            perPersonMinor = if (scope == AccountScope.All) totals.spentMinor / people else null,
            shareOfFamilyPercent =
                if (scope != AccountScope.All &&
                    !isSavingsScope &&
                    familyTotal > 0
                ) {
                    (totals.spentMinor * 100 / familyTotal).toInt()
                } else {
                    null
                },
            rows = rows,
            savings = savings,
            spikeThresholdPct = threshold,
        )
    }

    companion object {
        /** 365.25 / 12: scales a monthly average to a week or a custom range. */
        const val DAYS_PER_MONTH = 30.4375
        const val MONTHS_PER_YEAR = 12.0
    }
}
