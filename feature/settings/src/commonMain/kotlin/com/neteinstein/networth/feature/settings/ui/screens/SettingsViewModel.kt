package com.neteinstein.networth.feature.settings.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val dynamicColorAvailable: Boolean = false,
)

class SettingsViewModel(
    observeAppearance: ObserveAppearanceUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setDynamicColor: SetDynamicColorUseCase,
    dynamicColorAvailable: Boolean,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = observeAppearance()
        .map { SettingsUiState(appearance = it, dynamicColorAvailable = dynamicColorAvailable) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsUiState(dynamicColorAvailable = dynamicColorAvailable),
        )

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun onDynamicColorToggled(enabled: Boolean) {
        viewModelScope.launch { setDynamicColor(enabled) }
    }
}
