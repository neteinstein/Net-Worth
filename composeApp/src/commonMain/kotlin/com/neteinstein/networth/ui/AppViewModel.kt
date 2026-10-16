package com.neteinstein.networth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** App-shell state: currently just the appearance settings that drive the theme. */
class AppViewModel(
    observeAppearance: ObserveAppearanceUseCase,
) : ViewModel() {
    val appearance: StateFlow<AppearanceSettings> = observeAppearance()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())
}
