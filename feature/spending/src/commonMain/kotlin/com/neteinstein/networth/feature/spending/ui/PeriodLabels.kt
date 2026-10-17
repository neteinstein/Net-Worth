package com.neteinstein.networth.feature.spending.ui

import com.neteinstein.networth.feature.spending.domain.model.DateRange
import com.neteinstein.networth.feature.spending.domain.model.PeriodKind
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** Labels for a period, given the 12 localised short and long month names. Pure, so unit-tested. */
object PeriodLabels {
    /** The header pill: "Sep 2026", "22–28 Sep 2026", "2026", "3 Aug – 2 Sep 2026". */
    fun title(
        kind: PeriodKind,
        range: DateRange,
        short: List<String>,
    ): String =
        when (kind) {
            PeriodKind.MONTH -> "${short[range.start.month.number - 1]} ${range.start.year}"
            PeriodKind.YEAR -> range.start.year.toString()
            PeriodKind.WEEK, PeriodKind.CUSTOM -> span(range.start, range.end, short)
        }

    /** After the scope on the total card: "September", "22–28 Sep", "2026". */
    fun inline(
        kind: PeriodKind,
        range: DateRange,
        short: List<String>,
        long: List<String>,
    ): String =
        when (kind) {
            PeriodKind.MONTH -> long[range.start.month.number - 1]
            PeriodKind.YEAR -> range.start.year.toString()
            PeriodKind.WEEK, PeriodKind.CUSTOM -> span(range.start, range.end, short).removeSuffix(" ${range.end.year}")
        }

    private fun span(
        start: LocalDate,
        end: LocalDate,
        short: List<String>,
    ): String {
        val endText = "${end.day} ${short[end.month.number - 1]} ${end.year}"
        return when {
            start == end -> endText
            start.year == end.year && start.month == end.month -> "${start.day}–$endText"
            start.year == end.year -> "${start.day} ${short[start.month.number - 1]} – $endText"
            else -> "${start.day} ${short[start.month.number - 1]} ${start.year} – $endText"
        }
    }
}
