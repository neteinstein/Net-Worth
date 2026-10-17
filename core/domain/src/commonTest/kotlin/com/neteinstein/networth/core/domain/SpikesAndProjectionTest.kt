package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.TestFamily.groceries
import com.neteinstein.networth.core.domain.TestFamily.school
import com.neteinstein.networth.core.domain.calc.averageMonthlySavings
import com.neteinstein.networth.core.domain.calc.projectSavings
import com.neteinstein.networth.core.domain.calc.spikes
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.SeasonalMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpikesAndProjectionTest {
    private val sep = CalendarMonth(2026, 9)

    // School averages €285 over the previous months and costs €610 in September (+114 %), as on the canvas.
    private val history = listOf(
        tx("2026-07-10", -285_00, categoryId = school.id),
        tx("2026-08-10", -285_00, categoryId = school.id),
        tx("2026-07-11", -900_00, categoryId = groceries.id),
        tx("2026-08-11", -900_00, categoryId = groceries.id),
        tx("2026-09-10", -610_00, categoryId = school.id),
        tx("2026-09-11", -920_00, categoryId = groceries.id),
    )

    @Test
    fun flagsCategoriesAboveThreshold() {
        val spikes = TestFamily.database(history).spikes(sep)

        assertEquals(listOf(school.id), spikes.map { it.categoryId })
        val spike = spikes.single()
        assertEquals(114, spike.percentAboveAverage)
        assertEquals(2.14, spike.ratioToAverage, 0.01)
        assertNull(spike.sameMonthLastYearMinor)
    }

    @Test
    fun seasonalMarksAndDisabledWarningsSuppressSpikes() {
        val base = TestFamily.database(history)
        val seasonal = base.copy(
            settings = base.settings.copy(
                alerts = base.settings.alerts.copy(seasonalMarks = listOf(SeasonalMark(school.id, 9, 2026))),
            ),
        )
        val muted = base.copy(categories = base.categories.map { it.copy(warnOnSpike = false) })

        assertTrue(seasonal.spikes(sep).isEmpty())
        assertTrue(muted.spikes(sep).isEmpty())
    }

    @Test
    fun reportsSameMonthLastYearWhenAvailable() {
        val db = TestFamily.database(history + tx("2025-09-10", -580_00, categoryId = school.id))

        assertEquals(580_00, db.spikes(sep).single { it.categoryId == school.id }.sameMonthLastYearMinor)
    }

    @Test
    fun projectionCompoundsYearly() {
        // The canvas: €18,400 saved, €610/month, flat → €55,000 in 5 years.
        assertEquals(55_000_00, projectSavings(18_400_00, 610_00, 5).last())
        val withReturn = projectSavings(1_000_00, 0, 2, annualRatePct = 10.0)
        assertEquals(listOf(1_000_00L, 1_100_00L, 1_210_00L), withReturn)
    }

    @Test
    fun averageSavingsIncludesTheGivenMonth() {
        val db = TestFamily.database(
            listOf(tx("2026-08-01", 1_000_00), tx("2026-08-02", -400_00), tx("2026-09-01", 1_000_00), tx("2026-09-02", -800_00)),
        )

        assertEquals(400_00, db.averageMonthlySavings(sep))
    }
}
