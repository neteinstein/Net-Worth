package com.neteinstein.networth.core.domain.calc

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import kotlinx.datetime.LocalDate

/**
 * Balance of [account] on [asOf] (inclusive): the latest balance snapshot on or before that day,
 * plus the movements dated after the snapshot. Without a snapshot it is the sum of all movements.
 * Liabilities (mortgage, loan) are returned as the positive amount owed.
 */
fun FamilyDatabase.balanceOf(account: Account, asOf: LocalDate? = null): Long {
    val snapshot = balanceSnapshots
        .filter { it.accountId == account.id && (asOf == null || it.date <= asOf) }
        .maxWithOrNull(compareBy({ it.date }, { it.updatedAt }))
    val movements = transactions.filter {
        it.accountId == account.id &&
            (asOf == null || it.date <= asOf) &&
            (snapshot == null || it.date > snapshot.date)
    }
    return (snapshot?.amountMinor ?: 0L) + movements.sumOf { it.amountMinor }
}

data class NetWorth(
    val assetsMinor: Long,
    val liabilitiesMinor: Long,
    val balances: Map<String, Long>,
) {
    val netMinor: Long get() = assetsMinor - liabilitiesMinor
}

fun FamilyDatabase.netWorth(asOf: LocalDate? = null): NetWorth {
    val balances = accounts.associate { it.id to balanceOf(it, asOf) }
    val (liabilities, assets) = accounts.partition { it.type.isLiability }
    return NetWorth(
        assetsMinor = assets.sumOf { balances.getValue(it.id) },
        liabilitiesMinor = liabilities.sumOf { balances.getValue(it.id) },
        balances = balances,
    )
}

/** Accounts that hold savings (not day-to-day money, not debt). */
val AccountType.isSavings: Boolean
    get() = this == AccountType.SAVINGS || this == AccountType.TERM_DEPOSIT || this == AccountType.INVESTMENT

fun FamilyDatabase.savingsBalance(scope: AccountScope = AccountScope.All, asOf: LocalDate? = null): Long {
    val ids = scope.accountIds(this)
    return accounts.filter { it.id in ids && it.type.isSavings }.sumOf { balanceOf(it, asOf) }
}
