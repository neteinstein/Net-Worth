package com.neteinstein.networth.feature.alerts.ui

import com.neteinstein.networth.feature.alerts.domain.model.AlertsPeriod
import kotlinx.datetime.LocalDate

/** Labels for the Alerts period, given the 12 localised short month names. Pure, so unit-tested. */
object AlertsLabels {
    /** The header pill: "Sep 2026", "2026", [allTime], "3 Aug – 2 Sep 2026". */
    fun title(period: AlertsPeriod, short: List<String>, allTime: String): String = when (period) {
        is AlertsPeriod.Month -> "${short[period.month.month - 1]} ${period.month.year}"
        is AlertsPeriod.Year -> period.year.toString()
        AlertsPeriod.AllTime -> allTime
        is AlertsPeriod.Custom -> span(period.start, period.end, short)
    }

    private fun span(start: LocalDate, end: LocalDate, short: List<String>): String {
        val endText = "${end.day} ${short[end.month.ordinal]} ${end.year}"
        return when {
            start == end -> endText
            start.year == end.year && start.month == end.month -> "${start.day}–$endText"
            start.year == end.year -> "${start.day} ${short[start.month.ordinal]} – $endText"
            else -> "${start.day} ${short[start.month.ordinal]} ${start.year} – $endText"
        }
    }
}
