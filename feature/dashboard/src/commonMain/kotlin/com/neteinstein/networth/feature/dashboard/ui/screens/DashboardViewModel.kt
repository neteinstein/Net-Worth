package com.neteinstein.networth.feature.dashboard.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.feature.dashboard.domain.usecase.ObserveNetWorthUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    observeNetWorth: ObserveNetWorthUseCase,
) : ViewModel() {
    val state: StateFlow<DashboardUiState> = observeNetWorth()
        .map { DashboardUiState(isLoading = false, summary = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
