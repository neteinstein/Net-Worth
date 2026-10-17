package com.neteinstein.networth.feature.spending.domain.model

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.model.TransactionFilter
import com.neteinstein.networth.core.domain.model.TransactionTypeFilter
import kotlin.math.abs

/** Category id used in filters for "Not categorised yet". */
const val UNCATEGORISED = ""

/** A list of transactions to show: a filter, optionally limited to a period. */
data class TransactionQuery(
    val filter: TransactionFilter,
    val period: DateRange? = null,
)

/**
 * Whether [tx] passes [filter] (the Filters artboard):
 * - accounts / categories: empty means any; [UNCATEGORISED] stands for "no category";
 * - tagged for: empty means everyone, otherwise the transaction must be tagged for one of them;
 * - type: expenses are money out, income money in; transfers between own accounts only when asked;
 * - min/max compare the amount without its sign; text matches the description or the note.
 */
fun TransactionFilter.matches(tx: Transaction): Boolean {
    if (accountIds.isNotEmpty() && tx.accountId !in accountIds) return false
    if (categoryIds.isNotEmpty() && (tx.categoryId ?: UNCATEGORISED) !in categoryIds) return false
    if (memberIds.isNotEmpty() && tx.memberIds.none { it in memberIds }) return false
    if (tx.isInternalTransfer && !includeTransfers) return false
    when (type) {
        TransactionTypeFilter.EXPENSES -> if (tx.amountMinor >= 0) return false
        TransactionTypeFilter.INCOME -> if (tx.amountMinor <= 0) return false
        TransactionTypeFilter.BOTH -> Unit
    }
    val magnitude = abs(tx.amountMinor)
    minMinor?.let { if (magnitude < it) return false }
    maxMinor?.let { if (magnitude > it) return false }
    val needle = text?.trim()?.takeIf { it.isNotEmpty() } ?: return true
    return tx.description.contains(needle, ignoreCase = true) || tx.note?.contains(needle, ignoreCase = true) == true
}

/** Transactions for [query], newest first. */
fun FamilyDatabase.find(query: TransactionQuery): List<Transaction> =
    transactions
        .filter { query.filter.matches(it) && (query.period == null || it.date in query.period) }
        .sortedWith(compareByDescending<Transaction> { it.date }.thenBy { it.description })

/** How many choices differ from "everything" — the badge on the Filters chip. */
val TransactionFilter.activeCount: Int
    get() =
        listOf(
            accountIds.isNotEmpty(),
            categoryIds.isNotEmpty(),
            memberIds.isNotEmpty(),
            type != TransactionTypeFilter.EXPENSES,
            includeTransfers,
            minMinor != null || maxMinor != null,
            !text.isNullOrBlank(),
        ).count { it }

/** The "Adults" choice of "Tagged for": every adult in the household. */
fun FamilyDatabase.adultIds(): List<String> = members.filter { it.role == MemberRole.ADULT }.map { it.id }
