package com.neteinstein.networth.feature.settings.domain.repository

import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeAppearance(): Flow<AppearanceSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)
}
