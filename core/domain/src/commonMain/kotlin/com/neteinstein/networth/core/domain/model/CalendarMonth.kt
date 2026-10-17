package com.neteinstein.networth.core.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** A calendar month (e.g. September 2026), the unit most of the app reports in. */
data class CalendarMonth(
    val year: Int,
    val month: Int,
) : Comparable<CalendarMonth> {
    init {
        require(month in 1..12) { "month must be 1..12, was $month" }
    }

    val firstDay: LocalDate get() = LocalDate(year, month, 1)

    val lastDay: LocalDate get() = LocalDate(next().year, next().month, 1).let { LocalDate.fromEpochDays(it.toEpochDays() - 1) }

    fun plus(months: Int): CalendarMonth {
        val index = year * 12 + (month - 1) + months
        return CalendarMonth(index.floorDiv(12), index.mod(12) + 1)
    }

    fun next(): CalendarMonth = plus(1)

    fun previous(): CalendarMonth = plus(-1)

    operator fun contains(date: LocalDate): Boolean = date.year == year && date.month.number == month

    /** Months from this one to [other]; negative when [other] is earlier. */
    fun monthsUntil(other: CalendarMonth): Int = (other.year * 12 + other.month) - (year * 12 + month)

    override fun compareTo(other: CalendarMonth): Int = compareValuesBy(this, other, { it.year }, { it.month })

    companion object {
        fun of(date: LocalDate): CalendarMonth = CalendarMonth(date.year, date.month.number)
    }
}
