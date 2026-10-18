package com.neteinstein.networth.feature.alerts.ui

import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.feature.alerts.domain.model.AlertsPeriod
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AlertsLabelsTest {
    private val short = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    @Test
    fun titles() {
        assertEquals("Sep 2026", AlertsLabels.title(AlertsPeriod.Month(CalendarMonth(2026, 9)), short, "All time"))
        assertEquals("2025", AlertsLabels.title(AlertsPeriod.Year(2025), short, "All time"))
        assertEquals("All time", AlertsLabels.title(AlertsPeriod.AllTime, short, "All time"))
        assertEquals("3 Aug – 2 Sep 2026", AlertsLabels.title(custom("2026-08-03", "2026-09-02"), short, ""))
        assertEquals("1–15 Sep 2026", AlertsLabels.title(custom("2026-09-01", "2026-09-15"), short, ""))
        assertEquals("20 Dec 2025 – 5 Jan 2026", AlertsLabels.title(custom("2025-12-20", "2026-01-05"), short, ""))
        assertEquals("5 Jan 2026", AlertsLabels.title(custom("2026-01-05", "2026-01-05"), short, ""))
    }

    private fun custom(start: String, end: String) = AlertsPeriod.Custom(LocalDate.parse(start), LocalDate.parse(end))
}
