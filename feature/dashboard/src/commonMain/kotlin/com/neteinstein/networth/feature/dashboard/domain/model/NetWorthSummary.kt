package com.neteinstein.networth.feature.dashboard.domain.model

data class NetWorthSummary(
    val totalAssets: Money,
    val totalLiabilities: Money,
    val assets: List<Account>,
    val liabilities: List<Account>,
) {
    val netWorth: Money get() = totalAssets - totalLiabilities

    /** Share of gross assets financed by debt, 0.0–1.0+ (0 when there are no assets). */
    val debtRatio: Double
        get() = if (totalAssets.minorUnits == 0L) 0.0 else totalLiabilities.minorUnits.toDouble() / totalAssets.minorUnits

    companion object {
        /** Splits [accounts] into assets/liabilities (largest first) and totals each side. */
        fun from(accounts: List<Account>, currency: CurrencyCode = CurrencyCode.DEFAULT): NetWorthSummary {
            val (assets, liabilities) = accounts.partition { it.kind == AccountKind.ASSET }
            return NetWorthSummary(
                totalAssets = assets.total(currency),
                totalLiabilities = liabilities.total(currency),
                assets = assets.sortedByDescending { it.balance.minorUnits },
                liabilities = liabilities.sortedByDescending { it.balance.minorUnits },
            )
        }

        private fun List<Account>.total(currency: CurrencyCode): Money =
            fold(Money.zero(currency)) { total, account -> total + account.balance }

        fun empty(currency: CurrencyCode = CurrencyCode.DEFAULT) =
            NetWorthSummary(Money.zero(currency), Money.zero(currency), emptyList(), emptyList())
    }
}
