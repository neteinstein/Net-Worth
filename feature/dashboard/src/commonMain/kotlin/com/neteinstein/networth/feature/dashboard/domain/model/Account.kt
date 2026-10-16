package com.neteinstein.networth.feature.dashboard.domain.model

/** Something that holds value (asset) or owes it (liability). Balances are stored as positive. */
data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind,
    val category: AccountCategory,
    val balance: Money,
)

enum class AccountKind { ASSET, LIABILITY }

enum class AccountCategory(val kind: AccountKind) {
    CASH(AccountKind.ASSET),
    INVESTMENTS(AccountKind.ASSET),
    PROPERTY(AccountKind.ASSET),
    OTHER_ASSET(AccountKind.ASSET),
    MORTGAGE(AccountKind.LIABILITY),
    LOAN(AccountKind.LIABILITY),
    CREDIT_CARD(AccountKind.LIABILITY),
    OTHER_LIABILITY(AccountKind.LIABILITY),
}
