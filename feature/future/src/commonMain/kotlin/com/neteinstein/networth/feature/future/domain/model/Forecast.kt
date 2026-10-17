package com.neteinstein.networth.feature.future.domain.model

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.CalendarMonth

data class ForecastMonth(
    val month: CalendarMonth,
    val spendMinor: Long,
    /** Costs more than the family's average monthly income (only for all accounts). */
    val overIncome: Boolean,
)

/** "Dec · Christmas & gifts · €5,150": [reason] is a category name, or null when nothing stands out. */
data class Peak(
    val month: CalendarMonth,
    val reason: String?,
    val spendMinor: Long,
)

data class YearRow(
    val year: Int,
    val spendMinor: Long,
    val saveMinor: Long,
    /** Savings at the end of that year. */
    val balanceMinor: Long,
)

/** Where yearly savings head when income and prices keep growing at the assumed rates. */
enum class Trend { FLAT, SHRINKING, GROWING }

data class Forecast(
    val currencyCode: String,
    /** The latest month with data; the forecast covers the twelve after it. Null without history. */
    val latest: CalendarMonth?,
    val scope: AccountScope,
    val scopes: List<AccountScope>,
    val months: List<ForecastMonth>,
    /** Average monthly income; only shown for all accounts. */
    val monthlyIncomeMinor: Long?,
    val yearSpendMinor: Long,
    val lastYearSpendMinor: Long,
    val yearSaveMinor: Long?,
    val lastYearSaveMinor: Long?,
    /** For one account: its share of the family's predicted spending. */
    val shareOfFamilyPercent: Int?,
    val peaks: List<Peak>,
    /** Monthly amount that covers the months costing more than income, or null when none do. */
    val setAsideMinor: Long?,
    val coveredMonths: List<CalendarMonth>,
    val years: List<YearRow>,
    val trend: Trend,
    val trendPerYearMinor: Long = 0,
) {
    /** Predicted spending vs the last twelve months, in percent (+4 means 4 % more). */
    val changePercent: Int?
        get() = if (lastYearSpendMinor <= 0) null else ((yearSpendMinor - lastYearSpendMinor) * 100 / lastYearSpendMinor).toInt()

    companion object {
        fun empty(currencyCode: String, scopes: List<AccountScope>) = Forecast(
            currencyCode = currencyCode,
            latest = null,
            scope = AccountScope.All,
            scopes = scopes,
            months = emptyList(),
            monthlyIncomeMinor = null,
            yearSpendMinor = 0,
            lastYearSpendMinor = 0,
            yearSaveMinor = null,
            lastYearSaveMinor = null,
            shareOfFamilyPercent = null,
            peaks = emptyList(),
            setAsideMinor = null,
            coveredMonths = emptyList(),
            years = emptyList(),
            trend = Trend.FLAT,
        )
    }
}

/** Whole currency units (e.g. €100) as minor units for a currency with [fractionDigits]. */
internal fun unitsToMinor(units: Long, fractionDigits: Int): Long {
    var minor = units
    repeat(fractionDigits) { minor *= 10 }
    return minor
}
