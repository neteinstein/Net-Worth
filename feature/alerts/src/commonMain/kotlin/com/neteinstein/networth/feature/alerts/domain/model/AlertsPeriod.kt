package com.neteinstein.networth.feature.alerts.domain.model

import com.neteinstein.networth.core.domain.model.CalendarMonth
import kotlinx.datetime.LocalDate

/** What Alerts reports on: one month (the artboard), or every month of a longer period. */
sealed interface AlertsPeriod {
    data class Month(
        val month: CalendarMonth,
    ) : AlertsPeriod

    data class Year(
        val year: Int,
    ) : AlertsPeriod

    /** Everything imported, from the first movement to the last. */
    data object AllTime : AlertsPeriod

    /** Any inclusive date range the user picks. */
    data class Custom(
        val start: LocalDate,
        val end: LocalDate,
    ) : AlertsPeriod {
        init {
            require(start <= end) { "start must not be after end" }
        }
    }
}
