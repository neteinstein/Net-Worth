package com.neteinstein.networth.feature.future.domain.usecase

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.coveredMonths
import com.neteinstein.networth.core.domain.calc.latestMonth
import com.neteinstein.networth.core.domain.calc.savingsBalance
import com.neteinstein.networth.core.domain.calc.scopeChoices
import com.neteinstein.networth.core.domain.calc.totals
import com.neteinstein.networth.core.domain.calc.trailingAverage
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.feature.future.domain.model.Forecast
import com.neteinstein.networth.feature.future.domain.model.ForecastMonth
import com.neteinstein.networth.feature.future.domain.model.Peak
import com.neteinstein.networth.feature.future.domain.model.Trend
import com.neteinstein.networth.feature.future.domain.model.YearRow
import com.neteinstein.networth.feature.future.domain.model.unitsToMinor
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * The "Year ahead" artboard: the twelve months after the latest imported one, following the
 * family's own patterns. Each month costs what the same month did a year earlier (or the 12-month
 * average when that month has no data), raised by the price-rise assumption. The year-by-year table
 * grows income and prices by the household's assumptions from there.
 */
class BuildForecastUseCase {
    operator fun invoke(database: FamilyDatabase, scope: AccountScope = AccountScope.All): Forecast {
        val currency = database.settings.currencyCode
        val scopes = database.scopeChoices()
        val effective = scope.takeIf { it in scopes } ?: AccountScope.All
        val latest = database.latestMonth() ?: return Forecast.empty(currency, scopes)
        val assumptions = database.settings.assumptions
        val priceFactor = 1 + assumptions.priceRisePct / 100
        val fraction = database.currency.fractionDigits

        val familyMonths = forecastMonths(database, AccountScope.All, latest, priceFactor)
        val scopeMonths = if (effective == AccountScope.All) familyMonths else forecastMonths(database, effective, latest, priceFactor)
        val familyAverage = database.trailingAverage(AccountScope.All, latest.next())
        val isFamily = effective == AccountScope.All
        val income = familyAverage.incomeMinor.takeIf { isFamily }
        val months = scopeMonths.map { (month, spend) -> ForecastMonth(month, spend, income != null && spend > income) }

        val yearSpend = months.sumOf { it.spendMinor }
        val familyYearSpend = familyMonths.sumOf { it.second }
        val lastYearSpend = database.trailingAverage(effective, latest.next()).spentMinor * 12
        val over = months.filter { it.overIncome }
        val excess = income?.let { inc -> over.sumOf { it.spendMinor - inc } } ?: 0L

        return Forecast(
            currencyCode = currency,
            latest = latest,
            scope = effective,
            scopes = scopes,
            months = months,
            monthlyIncomeMinor = income,
            yearSpendMinor = yearSpend,
            lastYearSpendMinor = lastYearSpend,
            yearSaveMinor = income?.let { it * 12 - yearSpend },
            lastYearSaveMinor = familyAverage.savedMinor.takeIf { isFamily }?.let { it * 12 },
            shareOfFamilyPercent = if (isFamily || familyYearSpend <= 0) null else (yearSpend * 100 / familyYearSpend).toInt(),
            peaks = peaks(database, effective, months),
            setAsideMinor = if (excess > 0) roundUp(excess / 12, unitsToMinor(SET_ASIDE_STEP_UNITS, fraction)) else null,
            coveredMonths = over.map { it.month },
            years = years(database, latest, familyAverage.incomeMinor * 12, familyYearSpend),
            trend = Trend.FLAT,
        ).withTrend(unitsToMinor(FLAT_TREND_UNITS, fraction))
    }

    private fun forecastMonths(
        database: FamilyDatabase,
        scope: AccountScope,
        latest: CalendarMonth,
        priceFactor: Double,
    ): List<Pair<CalendarMonth, Long>> {
        val covered = database.coveredMonths()
        val average = database.trailingAverage(scope, latest.next()).spentMinor
        return (1..12).map { latest.plus(it) }.map { month ->
            val lastYear = month.plus(-12)
            val base = if (lastYear in covered) database.totals(scope, lastYear).spentMinor else average
            month to (base * priceFactor).roundToLong()
        }
    }

    /** Up to three months clearly above a typical one, in calendar order, with what drives each. */
    private fun peaks(database: FamilyDatabase, scope: AccountScope, months: List<ForecastMonth>): List<Peak> {
        if (months.isEmpty()) return emptyList()
        val typical = months.sumOf { it.spendMinor } / months.size
        return months
            .filter { it.spendMinor > typical }
            .sortedByDescending { it.spendMinor }
            .take(MAX_PEAKS)
            .sortedBy { it.month }
            .map { Peak(it.month, reason(database, scope, it.month), it.spendMinor) }
    }

    /**
     * Why a month peaks: a category marked as peaking that month, else one marked seasonal for it last
     * year, else the category that ran furthest above its average in the same month last year.
     */
    private fun reason(database: FamilyDatabase, scope: AccountScope, month: CalendarMonth): String? {
        database.categories.firstOrNull { it.seasonalPeakMonth == month.month }?.let { return it.name }
        val lastYear = month.plus(-12)
        database.settings.alerts.seasonalMarks
            .firstOrNull { it.month == lastYear.month && it.year == lastYear.year }
            ?.let { mark -> database.category(mark.categoryId)?.let { return it.name } }
        if (lastYear !in database.coveredMonths()) return null
        val spent = database.totals(scope, lastYear).spentByCategory
        val average = database.trailingAverage(scope, lastYear).spentByCategory
        val top = spent.entries
            .filter { it.key != null }
            .maxByOrNull { (id, value) -> value - (average[id] ?: 0L) }
            ?.takeIf { (id, value) -> value > (average[id] ?: 0L) }
        return top?.key?.let { database.category(it)?.name }
    }

    private fun years(database: FamilyDatabase, latest: CalendarMonth, yearIncome: Long, yearSpend: Long): List<YearRow> {
        val assumptions = database.settings.assumptions
        val growth = 1 + assumptions.incomeGrowthPct / 100
        val prices = 1 + assumptions.priceRisePct / 100
        var balance = database.savingsBalance()
        return (0 until YEARS).map { k ->
            val income = (yearIncome * growth.pow(k)).roundToLong()
            val spend = (yearSpend * prices.pow(k)).roundToLong()
            balance += income - spend
            YearRow(latest.year + 1 + k, spend, income - spend, balance)
        }
    }

    private fun Forecast.withTrend(flatBand: Long): Forecast {
        if (years.size < 2) return this
        val drift = years.last().saveMinor - years.first().saveMinor
        val trend = when {
            abs(drift) < flatBand -> Trend.FLAT
            drift < 0 -> Trend.SHRINKING
            else -> Trend.GROWING
        }
        return copy(trend = trend, trendPerYearMinor = abs(drift) / (years.size - 1))
    }

    private fun roundUp(value: Long, step: Long): Long = (value + step - 1) / step * step

    companion object {
        const val YEARS = 5
        const val MAX_PEAKS = 3

        /** Yearly savings that move less than this over five years read as "roughly flat". */
        const val FLAT_TREND_UNITS = 200L

        /** "Put aside €100 a month": rounded up to tens. */
        const val SET_ASIDE_STEP_UNITS = 10L
    }
}
