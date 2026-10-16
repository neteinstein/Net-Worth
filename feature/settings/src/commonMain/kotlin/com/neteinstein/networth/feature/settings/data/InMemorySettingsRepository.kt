package com.neteinstein.networth.feature.settings.data

import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-lifetime settings. Swap for a persistent store when persistence is specified. */
class InMemorySettingsRepository(
    initial: AppearanceSettings = AppearanceSettings(),
) : SettingsRepository {
    private val appearance = MutableStateFlow(initial)

    override fun observeAppearance(): Flow<AppearanceSettings> = appearance.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) = appearance.update { it.copy(themeMode = mode) }

    override suspend fun setDynamicColor(enabled: Boolean) = appearance.update { it.copy(useDynamicColor = enabled) }
}
