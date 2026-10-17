package com.neteinstein.networth.core.domain.calc

import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase

/** A category that cost noticeably more than usual in a month. */
data class Spike(
    val categoryId: String,
    val month: CalendarMonth,
    val spentMinor: Long,
    val averageMinor: Long,
    /** Spending in the same month a year earlier, when that month has data. */
    val sameMonthLastYearMinor: Long?,
) {
    /** How far above average, in percent (114 means +114 %). */
    val percentAboveAverage: Int get() = ((spentMinor - averageMinor) * 100 / averageMinor).toInt()

    /** e.g. 2.1 for "2.1× your 12-month average". */
    val ratioToAverage: Double get() = spentMinor.toDouble() / averageMinor
}

/**
 * Categories whose spending in [month] is at least the household's spike threshold above their
 * trailing average. Categories with spike warnings off or without history are skipped, and so are
 * those marked as seasonal for that month unless [includeSeasonal] (Alerts shows them as planned).
 * Largest excess first.
 */
fun FamilyDatabase.spikes(month: CalendarMonth, scope: AccountScope = AccountScope.All, includeSeasonal: Boolean = false): List<Spike> {
    val threshold = settings.alerts.spikeThresholdPct
    val current = totals(scope, month).spentByCategory
    val average = trailingAverage(scope, month).spentByCategory
    val lastYear = month.plus(-12)
    val lastYearTotals = if (lastYear in coveredMonths(scope)) totals(scope, lastYear).spentByCategory else null
    val seasonal = if (includeSeasonal) emptySet() else seasonalCategories(month)
    return categories
        .filter { it.warnOnSpike && it.id !in seasonal }
        .mapNotNull { category ->
            val spent = current[category.id] ?: return@mapNotNull null
            val avg = average[category.id]?.takeIf { it > 0 } ?: return@mapNotNull null
            if ((spent - avg) * 100 < threshold.toLong() * avg) return@mapNotNull null
            Spike(category.id, month, spent, avg, lastYearTotals?.let { it[category.id] ?: 0L })
        }
        .sortedByDescending { it.spentMinor - it.averageMinor }
}

/** Categories marked "seasonal" for [month]: their spike that month was expected. */
fun FamilyDatabase.seasonalCategories(month: CalendarMonth): Set<String> =
    settings.alerts.seasonalMarks.filter { it.month == month.month && it.year == month.year }.map { it.categoryId }.toSet()
