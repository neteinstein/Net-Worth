package com.neteinstein.networth.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Every destination in the app. Add new ones here *and* in [navSavedStateConfiguration]. */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Overview : Route

    @Serializable
    data object Manage : Route

    /** Manage with the projection assumptions editor open (from Future). */
    @Serializable
    data object ManageAssumptions : Route

    @Serializable
    data object Categories : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Import : Route

    @Serializable
    data object Review : Route

    @Serializable
    data object Spending : Route

    @Serializable
    data object Filters : Route

    /** A transaction list: one category over a period (dates as ISO strings), or the active filter. */
    @Serializable
    data class Transactions(
        val categoryId: String? = null,
        val start: String? = null,
        val end: String? = null,
        val accountIds: List<String> = emptyList(),
        val fromFilters: Boolean = false,
    ) : Route

    @Serializable
    data object Alerts : Route

    @Serializable
    data object Future : Route

    @Serializable
    data object YearAhead : Route
}
