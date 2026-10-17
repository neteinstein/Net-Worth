package com.neteinstein.networth.feature.importer.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** How a statement's period is described ("22–28 Sep 2026", "August 2026"). Pure, so it is unit-tested. */
object Periods {
    enum class Kind { WEEKLY, MONTHLY, OTHER }

    /**
     * [shortMonths]/[longMonths] are the 12 localised month names. A period covering one whole
     * calendar month (banks often end it on the 1st of the next) reads as that month's name.
     */
    fun label(
        start: LocalDate,
        end: LocalDate,
        shortMonths: List<String>,
        longMonths: List<String>,
    ): String {
        if (isWholeMonth(start, end)) return "${longMonths[start.month.number - 1]} ${start.year}"
        val endText = "${end.day} ${shortMonths[end.month.number - 1]} ${end.year}"
        return when {
            start == end -> endText
            start.year == end.year && start.month == end.month -> "${start.day}–$endText"
            start.year == end.year -> "${start.day} ${shortMonths[start.month.number - 1]} – $endText"
            else -> "${start.day} ${shortMonths[start.month.number - 1]} ${start.year} – $endText"
        }
    }

    fun kind(
        start: LocalDate,
        end: LocalDate,
    ): Kind {
        val days = end.toEpochDays() - start.toEpochDays() + 1
        return when {
            isWholeMonth(start, end) -> Kind.MONTHLY
            days in 6..8 -> Kind.WEEKLY
            else -> Kind.OTHER
        }
    }

    private fun isWholeMonth(
        start: LocalDate,
        end: LocalDate,
    ): Boolean {
        if (start.day != 1) return false
        val nextMonth = if (start.month.number == 12) LocalDate(start.year + 1, 1, 1) else LocalDate(start.year, start.month.number + 1, 1)
        val lastDay = LocalDate.fromEpochDays(nextMonth.toEpochDays() - 1)
        return end == lastDay || end == nextMonth
    }
}
