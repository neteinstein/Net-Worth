package com.neteinstein.networth.feature.spending.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/** The period chips on Spending: Week, Month, Year or a custom range. */
enum class PeriodKind { WEEK, MONTH, YEAR, CUSTOM }

/** Inclusive date range. */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    init {
        require(start <= end) { "start must not be after end" }
    }

    val days: Int get() = (end.toEpochDays() - start.toEpochDays() + 1).toInt()

    operator fun contains(date: LocalDate): Boolean = date >= start && date <= end

    companion object {
        /** The week (Monday–Sunday), month or year containing [date]. Custom ranges start as that month. */
        fun of(
            kind: PeriodKind,
            date: LocalDate,
        ): DateRange =
            when (kind) {
                PeriodKind.WEEK -> {
                    val monday = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber))
                    DateRange(monday, monday.plus(DatePeriod(days = 6)))
                }
                PeriodKind.YEAR -> DateRange(LocalDate(date.year, 1, 1), LocalDate(date.year, 12, 31))
                PeriodKind.MONTH, PeriodKind.CUSTOM -> {
                    val first = LocalDate(date.year, date.month.number, 1)
                    DateRange(first, first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)))
                }
            }
    }

    /** The previous (steps = -1) or next (+1) period of the same kind; custom ranges move by their length. */
    fun shift(
        kind: PeriodKind,
        steps: Int,
    ): DateRange =
        when (kind) {
            PeriodKind.WEEK -> DateRange(start.plus(DatePeriod(days = 7 * steps)), end.plus(DatePeriod(days = 7 * steps)))
            PeriodKind.MONTH -> of(PeriodKind.MONTH, start.plus(DatePeriod(months = steps)))
            PeriodKind.YEAR -> of(PeriodKind.YEAR, start.plus(DatePeriod(years = steps)))
            PeriodKind.CUSTOM -> DateRange(start.plus(DatePeriod(days = days * steps)), end.plus(DatePeriod(days = days * steps)))
        }
}
