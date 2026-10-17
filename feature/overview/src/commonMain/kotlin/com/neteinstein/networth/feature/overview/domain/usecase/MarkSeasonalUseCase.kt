package com.neteinstein.networth.feature.overview.domain.usecase

import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.SeasonalMark
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase

/**
 * "Mark as seasonal": this month's spike in a category is expected (e.g. back to school), so it is
 * no longer flagged, and the category remembers the month as its seasonal peak.
 */
class MarkSeasonalUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(categoryId: String, month: CalendarMonth) = edit { database, now ->
        val alerts = database.settings.alerts
        val mark = SeasonalMark(categoryId, month.month, month.year)
        database.copy(
            settings = database.settings.copy(
                alerts = alerts.copy(seasonalMarks = (alerts.seasonalMarks + mark).distinct()),
                updatedAt = now,
            ),
            categories = database.categories.map {
                if (it.id == categoryId) it.copy(seasonalPeakMonth = month.month, updatedAt = now) else it
            },
        )
    }
}
