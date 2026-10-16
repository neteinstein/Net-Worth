package com.neteinstein.networth.feature.settings.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
) {
    /** Resolves the effective dark/light choice given what the OS currently reports. */
    fun isDark(systemIsDark: Boolean): Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
