package com.neteinstein.networth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.RestoreSessionUseCase
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Top-level screen: a spinner while restoring, onboarding until a database is open, then the app. */
enum class RootDestination { LOADING, ONBOARDING, MAIN }

/** App-shell state: the theme, and whether to show onboarding or the app. */
class AppViewModel(
    observeAppearance: ObserveAppearanceUseCase,
    observeAuth: ObserveAuthUseCase,
    observeDatabase: ObserveDatabaseUseCase,
    restoreSession: RestoreSessionUseCase,
) : ViewModel() {
    val appearance: StateFlow<AppearanceSettings> = observeAppearance()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())

    val destination: StateFlow<RootDestination> = combine(observeAuth(), observeDatabase()) { auth, database ->
        when {
            auth is AuthState.Unknown -> RootDestination.LOADING
            auth is AuthState.SignedOut -> RootDestination.ONBOARDING
            database is DatabaseState.Ready -> RootDestination.MAIN
            database is DatabaseState.Loading -> RootDestination.LOADING
            else -> RootDestination.ONBOARDING
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RootDestination.LOADING)

    init {
        viewModelScope.launch { restoreSession() }
    }
}
