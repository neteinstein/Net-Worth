package com.neteinstein.networth.feature.future.ui.screens

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.projectSavings
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.feature.future.domain.model.EmergencyFund
import com.neteinstein.networth.feature.future.domain.model.Forecast
import com.neteinstein.networth.feature.future.domain.model.ForecastMonth
import com.neteinstein.networth.feature.future.domain.model.Future
import com.neteinstein.networth.feature.future.domain.model.FutureScope
import com.neteinstein.networth.feature.future.domain.model.KidProjection
import com.neteinstein.networth.feature.future.domain.model.Peak
import com.neteinstein.networth.feature.future.domain.model.SetAside
import com.neteinstein.networth.feature.future.domain.model.Trend
import com.neteinstein.networth.feature.future.domain.model.WhatIfs
import com.neteinstein.networth.feature.future.domain.model.YearRow

/** Figures in the spirit of the artboards for previews (invented, never seed data). */
internal fun previewFutureState(scope: FutureScope = FutureScope.ALL): FutureUiState {
    val oct = CalendarMonth(2026, 10)
    val (start, monthly) = when (scope) {
        FutureScope.ALL -> 18_400_00L to 610_00L
        FutureScope.JOINT -> 12_600_00L to 520_00L
        FutureScope.KIDS -> 5_800_00L to 90_00L
    }
    val future = Future(
        currencyCode = "EUR",
        latest = oct,
        scope = scope,
        scopes = FutureScope.entries,
        monthlyMinor = monthly,
        flat = projectSavings(start, monthly, Future.YEARS),
        withReturn = projectSavings(start, monthly, Future.YEARS, 3.0),
        returnRatePct = 3.0,
        kids = listOf("Inês" to 2_300_00L, "Tomás" to 1_950_00L, "Rita" to 1_550_00L).map { (name, now) ->
            KidProjection(name, name, now, projectSavings(now, 30_00, Future.YEARS, 3.0))
        },
        diningCutMinor = 58_00,
        extraMinor = 100_00,
        setAside = SetAside("School", 9, 27_00),
        whatIfs = WhatIfs(cutDining = true),
        emergencyFund = EmergencyFund(6, 25_440_00, 12_600_00, CalendarMonth(2028, 11)),
        tenYearSpendMinor = 508_800_00,
        tenYearSpendWithPricesMinor = 570_029_00,
        priceRisePct = 2.5,
    )
    return FutureUiState(future, peakMonths = listOf(CalendarMonth(2026, 12), CalendarMonth(2027, 7), CalendarMonth(2027, 9)))
}

internal fun previewYearAheadState(scope: AccountScope = AccountScope.All): YearAheadUiState {
    val oct = CalendarMonth(2026, 10)
    val raw = listOf(4050, 5150, 4300, 3850, 3950, 4000, 4050, 4300, 5750, 4800, 4750, 4100)
    val family = scope == AccountScope.All
    val months = raw.mapIndexed { i, v -> ForecastMonth(oct.plus(i + 1), v * 100L, family && v > 4850) }
    val forecast = Forecast(
        currencyCode = "EUR",
        latest = oct,
        scope = scope,
        scopes = listOf(AccountScope.All, AccountScope.Single("a1"), AccountScope.Single("a2"), AccountScope.Children),
        months = months,
        monthlyIncomeMinor = if (family) 4_850_00 else null,
        yearSpendMinor = 53_050_00,
        lastYearSpendMinor = 50_880_00,
        yearSaveMinor = if (family) 5_150_00 else null,
        lastYearSaveMinor = if (family) 7_320_00 else null,
        shareOfFamilyPercent = if (family) null else 62,
        peaks = listOf(
            Peak(oct.plus(2), "Other", 5_150_00),
            Peak(oct.plus(9), "Vacations", 5_750_00),
            Peak(oct.plus(11), "School", 4_750_00),
        ),
        setAsideMinor = if (family) 100_00 else null,
        coveredMonths = if (family) listOf(oct.plus(2), oct.plus(9)) else emptyList(),
        years = listOf(
            YearRow(2027, 53_050_00, 5_150_00, 23_550_00),
            YearRow(2028, 54_376_00, 5_000_00, 28_550_00),
            YearRow(2029, 55_736_00, 4_816_00, 33_366_00),
            YearRow(2030, 57_129_00, 4_634_00, 38_000_00),
            YearRow(2031, 58_557_00, 4_440_00, 42_440_00),
        ),
        trend = Trend.SHRINKING,
        trendPerYearMinor = 178_00,
    )
    return YearAheadUiState(forecast, mapOf("a1" to "Pedro", "a2" to "Maria"))
}
