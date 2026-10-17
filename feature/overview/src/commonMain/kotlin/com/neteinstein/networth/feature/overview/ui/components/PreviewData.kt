package com.neteinstein.networth.feature.overview.ui.components

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.feature.overview.domain.usecase.AccountLine
import com.neteinstein.networth.feature.overview.domain.usecase.CategoryShare
import com.neteinstein.networth.feature.overview.domain.usecase.Overview
import com.neteinstein.networth.feature.overview.domain.usecase.SpikeSummary
import com.neteinstein.networth.feature.overview.domain.usecase.TrendPoint

/** The figures on the Overview artboard, for @Preview only. */
internal fun previewOverview(): Overview {
    val sep = CalendarMonth(2026, 9)
    val shares = listOf(
        Triple("Housing", "#141B2D", 1180),
        Triple("Groceries", "#2D55D6", 920),
        Triple("School", "#D9652B", 610),
        Triple("Transport", "#7D9BF2", 395),
        Triple("Clothes", "#A8487A", 285),
        Triple("Kids’ activities", "#F0A964", 240),
        Triple("Dining & leisure", "#6A4FC9", 230),
        Triple("Health", "#3E8C7E", 145),
        Triple("Other", "#A3ABBA", 115),
    ).map { (name, color, euros) -> CategoryShare(name, name, color, euros * 100L, euros * 100 / 4120) }
    val trend = listOf(3890, 3960, 4180, 5620, 4710, 4120).mapIndexed { i, euros -> TrendPoint(sep.plus(i - 5), euros * 100L, i == 5) }
    return Overview(
        householdName = "The Vicentes",
        adults = 2,
        children = 3,
        currencyCode = "EUR",
        month = sep,
        months = listOf(sep, sep.previous()),
        scopes = listOf(AccountScope.All, AccountScope.Single("a1"), AccountScope.Children),
        scope = AccountScope.All,
        hasTransactions = true,
        needsReview = 18,
        incomeMinor = 4_850_00,
        spentMinor = 4_120_00,
        savedMinor = 730_00,
        savedPercent = 15,
        spike = SpikeSummary("School", "School", 610_00, 285_00, 2.14),
        shares = shares,
        trend = trend,
        averageSpentMinor = 4_240_00,
        netWorthMinor = 22_400_00,
        accounts = listOf(
            AccountLine("a1", "Pedro", AccountType.CURRENT, 2_140_00, false),
            AccountLine("a2", "Joint savings", AccountType.SAVINGS, 12_600_00, false),
            AccountLine("c1", "Inês", AccountType.SAVINGS, 2_300_00, true),
        ),
        projectedSavingsMinor = 55_000_00,
    )
}
