package com.neteinstein.networth.feature.overview.ui.components

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.feature.overview.domain.model.OverviewPeriod
import com.neteinstein.networth.feature.overview.domain.model.TrendStep
import com.neteinstein.networth.feature.overview.domain.usecase.AccountLine
import com.neteinstein.networth.feature.overview.domain.usecase.CategoryShare
import com.neteinstein.networth.feature.overview.domain.usecase.Overview
import com.neteinstein.networth.feature.overview.domain.usecase.SpikeSummary
import com.neteinstein.networth.feature.overview.domain.usecase.TrendPoint
import kotlinx.datetime.LocalDate

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
        period = OverviewPeriod.Month(sep),
        from = sep.firstDay,
        to = sep.lastDay,
        month = sep,
        months = listOf(sep, sep.previous()),
        years = listOf(2026),
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

/** The same household over all time: a year and a half grouped by quarter, no spike. */
internal fun previewAllTimeOverview(): Overview {
    val first = CalendarMonth(2025, 4)
    val trend = listOf(11_870, 12_310, 13_040, 12_650, 14_510, 4_120).mapIndexed { i, euros ->
        TrendPoint(first.plus(i * 3), euros * 100L, isSelected = false, step = TrendStep.QUARTER)
    }
    val month = previewOverview()
    return month.copy(
        period = OverviewPeriod.AllTime,
        from = LocalDate(2025, 4, 1),
        to = LocalDate(2026, 9, 30),
        years = listOf(2026, 2025),
        incomeMinor = 87_300_00,
        spentMinor = 68_500_00,
        savedMinor = 18_800_00,
        savedPercent = 21,
        spike = null,
        shares = month.shares.map { it.copy(amountMinor = it.amountMinor * 68_500 / 4_120) },
        trend = trend,
        averageSpentMinor = 3_805_00,
    )
}
