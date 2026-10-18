package com.neteinstein.networth.feature.overview.ui

import com.neteinstein.networth.feature.overview.domain.model.OverviewPeriod
import com.neteinstein.networth.feature.overview.domain.model.TrendStep
import com.neteinstein.networth.feature.overview.domain.usecase.TrendPoint
import kotlinx.datetime.LocalDate

/** Labels for Overview's period and trend bars, given the 12 localised short and long month names. Pure, so unit-tested. */
object OverviewLabels {
    /** The header pill: "Sep 2026", "2026", [allTime], "3 Aug – 2 Sep 2026". */
    fun title(period: OverviewPeriod, short: List<String>, allTime: String): String = when (period) {
        is OverviewPeriod.Month -> "${short[period.month.month - 1]} ${period.month.year}"
        is OverviewPeriod.Year -> period.year.toString()
        OverviewPeriod.AllTime -> allTime
        is OverviewPeriod.Custom -> span(period.start, period.end, short)
    }

    /** After "Saved in": "September", "2026", "3 Aug – 2 Sep 2026"; null for all time, which has its own sentence. */
    fun inline(period: OverviewPeriod, short: List<String>, long: List<String>): String? = when (period) {
        is OverviewPeriod.Month -> long[period.month.month - 1]
        is OverviewPeriod.Year -> period.year.toString()
        OverviewPeriod.AllTime -> null
        is OverviewPeriod.Custom -> span(period.start, period.end, short)
    }

    /** Under a trend bar: "Sep", "Q3 ’26" (with the localised [quarter] letter) or "2026". */
    fun bar(point: TrendPoint, short: List<String>, quarter: String): String = when (point.step) {
        TrendStep.MONTH -> short[point.month.month - 1]
        TrendStep.QUARTER -> "$quarter${(point.month.month - 1) / 3 + 1} ’${(point.month.year % 100).toString().padStart(2, '0')}"
        TrendStep.YEAR -> point.month.year.toString()
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
