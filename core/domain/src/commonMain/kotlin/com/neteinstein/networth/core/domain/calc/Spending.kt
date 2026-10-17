package com.neteinstein.networth.core.domain.calc

import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.Transaction
import kotlinx.datetime.LocalDate

/**
 * Income and spending for one period.
 *
 * Rules (shared by every screen so numbers always agree):
 * - Internal transfers between the family's accounts are neither income nor spending.
 * - A categorised movement is spending in that category; money coming back in a category (a refund)
 *   lowers that category's spending.
 * - An uncategorised outflow is spending under the `null` category; an uncategorised inflow is income.
 */
data class PeriodTotals(
    val incomeMinor: Long,
    val spentByCategory: Map<String?, Long>,
) {
    val spentMinor: Long get() = spentByCategory.values.sum()
    val savedMinor: Long get() = incomeMinor - spentMinor

    /** Saved as a share of income, 0–100, or null without income. */
    val savedPercent: Int?
        get() = if (incomeMinor <= 0) null else (savedMinor * 100 / incomeMinor).toInt()

    companion object {
        val EMPTY = PeriodTotals(0, emptyMap())
    }
}

fun FamilyDatabase.transactionsIn(scope: AccountScope, from: LocalDate, to: LocalDate): List<Transaction> {
    val ids = scope.accountIds(this)
    return transactions.filter { it.accountId in ids && it.date >= from && it.date <= to }
}

fun totalsOf(transactions: List<Transaction>): PeriodTotals {
    var income = 0L
    val spent = mutableMapOf<String?, Long>()
    transactions.filterNot { it.isInternalTransfer }.forEach { tx ->
        when {
            tx.categoryId != null -> spent[tx.categoryId] = (spent[tx.categoryId] ?: 0L) - tx.amountMinor
            tx.amountMinor < 0 -> spent[null] = (spent[null] ?: 0L) - tx.amountMinor
            else -> income += tx.amountMinor
        }
    }
    return PeriodTotals(income, spent.filterValues { it != 0L })
}

fun FamilyDatabase.totals(scope: AccountScope, month: CalendarMonth): PeriodTotals =
    totalsOf(transactionsIn(scope, month.firstDay, month.lastDay))

/** Months that have at least one movement in [scope] — the history averages are based on. */
fun FamilyDatabase.coveredMonths(scope: AccountScope = AccountScope.All): Set<CalendarMonth> {
    val ids = scope.accountIds(this)
    return transactions.filter { it.accountId in ids }.map { CalendarMonth.of(it.date) }.toSet()
}

/** The most recent month with data, or null for an empty database. */
fun FamilyDatabase.latestMonth(scope: AccountScope = AccountScope.All): CalendarMonth? = coveredMonths(scope).maxOrNull()

/**
 * Average monthly totals over the up-to-12 months before [month] that have data. Months without
 * any imported movements are skipped so a short history is not diluted with zeros.
 */
fun FamilyDatabase.trailingAverage(scope: AccountScope, month: CalendarMonth, window: Int = 12): PeriodTotals {
    val covered = coveredMonths(scope)
    val months = (1..window).map { month.plus(-it) }.filter { it in covered }
    if (months.isEmpty()) return PeriodTotals.EMPTY
    val all = months.map { totals(scope, it) }
    val categories = all.flatMap { it.spentByCategory.keys }.toSet()
    return PeriodTotals(
        incomeMinor = all.sumOf { it.incomeMinor } / months.size,
        spentByCategory = categories.associateWith { key -> all.sumOf { it.spentByCategory[key] ?: 0L } / months.size },
    )
}

/** Spending per month for the [count] months ending at [last] (oldest first). */
fun FamilyDatabase.monthlySpending(scope: AccountScope, last: CalendarMonth, count: Int): List<Pair<CalendarMonth, Long>> =
    (count - 1 downTo 0).map { last.plus(-it) }.map { it to totals(scope, it).spentMinor }
