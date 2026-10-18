package com.neteinstein.networth.feature.settings.domain.usecase

import com.neteinstein.networth.feature.settings.domain.model.AppLanguage
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveAppearanceUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<AppearanceSettings> = repository.observeAppearance()
}

class SetThemeModeUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class SetDynamicColorUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setDynamicColor(enabled)
}

class SetLanguageUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}
