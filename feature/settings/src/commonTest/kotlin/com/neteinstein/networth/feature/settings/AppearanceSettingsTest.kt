package com.neteinstein.networth.feature.settings

import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppearanceSettingsTest {
    @Test
    fun systemModeFollowsTheOs() {
        assertTrue(AppearanceSettings(ThemeMode.SYSTEM).isDark(systemIsDark = true))
        assertFalse(AppearanceSettings(ThemeMode.SYSTEM).isDark(systemIsDark = false))
    }

    @Test
    fun explicitModesIgnoreTheOs() {
        assertTrue(AppearanceSettings(ThemeMode.DARK).isDark(systemIsDark = false))
        assertFalse(AppearanceSettings(ThemeMode.LIGHT).isDark(systemIsDark = true))
    }
}
