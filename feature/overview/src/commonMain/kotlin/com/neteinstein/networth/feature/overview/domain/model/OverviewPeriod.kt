package com.neteinstein.networth.feature.overview.domain.model

import com.neteinstein.networth.core.domain.model.CalendarMonth
import kotlinx.datetime.LocalDate

/** What Overview reports on: one month (the artboard), or an aggregate over a longer period. */
sealed interface OverviewPeriod {
    data class Month(
        val month: CalendarMonth,
    ) : OverviewPeriod

    data class Year(
        val year: Int,
    ) : OverviewPeriod

    /** Everything imported, from the first movement to the last. */
    data object AllTime : OverviewPeriod

    /** Any inclusive date range the user picks. */
    data class Custom(
        val start: LocalDate,
        val end: LocalDate,
    ) : OverviewPeriod {
        init {
            require(start <= end) { "start must not be after end" }
        }
    }

    val isMonth: Boolean get() = this is Month
}

/** The width of one bar in the spending trend. */
enum class TrendStep(
    val months: Int,
) {
    MONTH(1),
    QUARTER(3),
    YEAR(12),
}
