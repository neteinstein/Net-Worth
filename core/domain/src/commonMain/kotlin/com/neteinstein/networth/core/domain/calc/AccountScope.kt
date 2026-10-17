package com.neteinstein.networth.core.domain.calc

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MemberRole

/** Which accounts a figure is about — the account chips on Overview, Spending and Year ahead. */
sealed interface AccountScope {
    data object All : AccountScope

    data class Single(val accountId: String) : AccountScope

    /** Every account owned only by children (the design's "Kids’ savings"). */
    data object Children : AccountScope

    fun accountIds(database: FamilyDatabase): Set<String> = when (this) {
        All -> database.accounts.map { it.id }.toSet()
        is Single -> setOf(accountId)
        Children -> database.accounts.filter { database.isChildAccount(it) }.map { it.id }.toSet()
    }
}

/** True when every owner of [account] is a child member. */
fun FamilyDatabase.isChildAccount(account: Account): Boolean =
    account.ownerIds.isNotEmpty() && account.ownerIds.all { member(it)?.role == MemberRole.CHILD }

/**
 * The chips to show: all accounts, then each account not owned only by children (cash, joint
 * savings…), then one "children" chip grouping the kids' accounts when there are any. Liabilities
 * never appear because they have no spending.
 */
fun FamilyDatabase.scopeChoices(): List<AccountScope> = buildList {
    add(AccountScope.All)
    accounts.filter { !it.type.isLiability && !isChildAccount(it) }.forEach { add(AccountScope.Single(it.id)) }
    if (accounts.any { isChildAccount(it) }) add(AccountScope.Children)
}
