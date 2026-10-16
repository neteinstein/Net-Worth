package com.neteinstein.networth.feature.dashboard.data

import com.neteinstein.networth.feature.dashboard.domain.model.Account
import com.neteinstein.networth.feature.dashboard.domain.model.AccountCategory
import com.neteinstein.networth.feature.dashboard.domain.model.Money

/** Demo data for the scaffold. Not product content — replace once the real data source exists. */
object SampleAccounts {
    val all: List<Account> = listOf(
        account("checking", "Everyday account", AccountCategory.CASH, 4_250_00),
        account("savings", "Emergency fund", AccountCategory.CASH, 12_000_00),
        account("etf", "ETF portfolio", AccountCategory.INVESTMENTS, 38_740_55),
        account("home", "Home", AccountCategory.PROPERTY, 245_000_00),
        account("mortgage", "Mortgage", AccountCategory.MORTGAGE, 168_300_00),
        account("card", "Credit card", AccountCategory.CREDIT_CARD, 1_120_40),
    )

    private fun account(id: String, name: String, category: AccountCategory, cents: Long) =
        Account(id = id, name = name, kind = category.kind, category = category, balance = Money(cents))
}
