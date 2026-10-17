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

    @Serializable
    data object Categories : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Import : Route

    @Serializable
    data object Review : Route
}
