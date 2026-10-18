package com.neteinstein.networth.feature.overview.ui

import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.feature.overview.domain.model.OverviewPeriod
import com.neteinstein.networth.feature.overview.domain.model.TrendStep
import com.neteinstein.networth.feature.overview.domain.usecase.TrendPoint
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OverviewLabelsTest {
    private val short = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val long = short.map { "${it}ember" }
    private val sep = CalendarMonth(2026, 9)

    @Test
    fun titles() {
        assertEquals("Sep 2026", OverviewLabels.title(OverviewPeriod.Month(sep), short, "All time"))
        assertEquals("2025", OverviewLabels.title(OverviewPeriod.Year(2025), short, "All time"))
        assertEquals("All time", OverviewLabels.title(OverviewPeriod.AllTime, short, "All time"))
        assertEquals("3 Aug – 2 Sep 2026", OverviewLabels.title(custom("2026-08-03", "2026-09-02"), short, ""))
        assertEquals("1–15 Sep 2026", OverviewLabels.title(custom("2026-09-01", "2026-09-15"), short, ""))
        assertEquals("20 Dec 2025 – 5 Jan 2026", OverviewLabels.title(custom("2025-12-20", "2026-01-05"), short, ""))
        assertEquals("5 Jan 2026", OverviewLabels.title(custom("2026-01-05", "2026-01-05"), short, ""))
    }

    @Test
    fun inlineLabels() {
        assertEquals("Sepember", OverviewLabels.inline(OverviewPeriod.Month(sep), short, long))
        assertEquals("2026", OverviewLabels.inline(OverviewPeriod.Year(2026), short, long))
        assertEquals("3 Aug – 2 Sep 2026", OverviewLabels.inline(custom("2026-08-03", "2026-09-02"), short, long))
        assertNull(OverviewLabels.inline(OverviewPeriod.AllTime, short, long))
    }

    @Test
    fun barLabels() {
        assertEquals("Sep", OverviewLabels.bar(TrendPoint(sep, 0, false), short, "Q"))
        assertEquals("Q3 ’26", OverviewLabels.bar(TrendPoint(CalendarMonth(2026, 7), 0, false, TrendStep.QUARTER), short, "Q"))
        assertEquals("T4 ’05", OverviewLabels.bar(TrendPoint(CalendarMonth(2005, 10), 0, false, TrendStep.QUARTER), short, "T"))
        assertEquals("2026", OverviewLabels.bar(TrendPoint(CalendarMonth(2026, 1), 0, false, TrendStep.YEAR), short, "Q"))
    }

    private fun custom(start: String, end: String) = OverviewPeriod.Custom(LocalDate.parse(start), LocalDate.parse(end))
}
