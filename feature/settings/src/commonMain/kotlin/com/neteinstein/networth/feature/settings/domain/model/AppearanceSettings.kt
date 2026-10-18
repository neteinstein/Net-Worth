package com.neteinstein.networth.feature.settings.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The app's display language; [SYSTEM] follows the device. [tag] is the BCP 47 tag to force, if any. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    PORTUGUESE("pt"),
}

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
) {
    /** Resolves the effective dark/light choice given what the OS currently reports. */
    fun isDark(systemIsDark: Boolean): Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
