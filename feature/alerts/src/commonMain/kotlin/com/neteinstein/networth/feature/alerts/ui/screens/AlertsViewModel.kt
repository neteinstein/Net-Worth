package com.neteinstein.networth.feature.alerts.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.SetSeasonalUseCase
import com.neteinstein.networth.feature.alerts.domain.usecase.Alerts
import com.neteinstein.networth.feature.alerts.domain.usecase.BuildAlertsUseCase
import com.neteinstein.networth.feature.alerts.domain.usecase.SpikeAlert
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlertsUiState(
    val alerts: Alerts? = null,
)

class AlertsViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    private val buildAlerts: BuildAlertsUseCase,
    private val setSeasonal: SetSeasonalUseCase,
) : ViewModel() {
    val state: StateFlow<AlertsUiState> =
        observeDatabase()
            .map { db -> AlertsUiState((db as? DatabaseState.Ready)?.database?.let(buildAlerts::invoke)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    /** "Mark as seasonal" toggles: a planned spike stops counting as one (and can be unmarked). */
    fun onToggleSeasonal(spike: SpikeAlert) {
        viewModelScope.launch { setSeasonal(spike.categoryId, spike.month, !spike.seasonal) }
    }
}
