package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.model.CalendarMonth
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarMonthTest {
    @Test
    fun arithmeticCrossesYearBoundaries() {
        assertEquals(CalendarMonth(2027, 1), CalendarMonth(2026, 12).next())
        assertEquals(CalendarMonth(2025, 12), CalendarMonth(2026, 1).previous())
        assertEquals(CalendarMonth(2025, 9), CalendarMonth(2026, 9).plus(-12))
        assertEquals(14, CalendarMonth(2025, 11).monthsUntil(CalendarMonth(2027, 1)))
    }

    @Test
    fun boundsAndMembership() {
        val feb = CalendarMonth(2028, 2)
        assertEquals(LocalDate(2028, 2, 1), feb.firstDay)
        assertEquals(LocalDate(2028, 2, 29), feb.lastDay)
        assertTrue(LocalDate(2028, 2, 15) in feb)
        assertFalse(LocalDate(2028, 3, 1) in feb)
        assertEquals(feb, CalendarMonth.of(LocalDate(2028, 2, 10)))
    }

    @Test
    fun ordersChronologically() {
        assertTrue(CalendarMonth(2025, 12) < CalendarMonth(2026, 1))
        assertFailsWith<IllegalArgumentException> { CalendarMonth(2026, 13) }
    }
}
