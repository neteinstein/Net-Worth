package com.neteinstein.networth.core.domain.usecase

import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.SeasonalMark

/**
 * Marks (or unmarks) a category's spending in a month as seasonal — expected, like back to school —
 * so it stops being flagged as a spike. Marking also records the month as the category's peak.
 */
class SetSeasonalUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(categoryId: String, month: CalendarMonth, seasonal: Boolean) = edit { database, now ->
        val alerts = database.settings.alerts
        val mark = SeasonalMark(categoryId, month.month, month.year)
        val marks = if (seasonal) (alerts.seasonalMarks + mark).distinct() else alerts.seasonalMarks - mark
        database.copy(
            settings = database.settings.copy(alerts = alerts.copy(seasonalMarks = marks), updatedAt = now),
            categories = database.categories.map {
                if (seasonal && it.id == categoryId) it.copy(seasonalPeakMonth = month.month, updatedAt = now) else it
            },
        )
    }
}
