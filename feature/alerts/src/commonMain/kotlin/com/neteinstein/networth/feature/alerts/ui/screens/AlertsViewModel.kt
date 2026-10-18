package com.neteinstein.networth.feature.alerts.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.SetSeasonalUseCase
import com.neteinstein.networth.feature.alerts.domain.model.AlertsPeriod
import com.neteinstein.networth.feature.alerts.domain.usecase.Alerts
import com.neteinstein.networth.feature.alerts.domain.usecase.BuildAlertsUseCase
import com.neteinstein.networth.feature.alerts.domain.usecase.SpikeAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class AlertsUiState(
    val alerts: Alerts? = null,
)

class AlertsViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    private val buildAlerts: BuildAlertsUseCase,
    private val setSeasonal: SetSeasonalUseCase,
) : ViewModel() {
    /** Null until the user picks one: follow the latest month that has data. */
    private val selectedPeriod = MutableStateFlow<AlertsPeriod?>(null)

    val state: StateFlow<AlertsUiState> =
        combine(observeDatabase(), selectedPeriod) { db, period ->
            AlertsUiState((db as? DatabaseState.Ready)?.database?.let { buildAlerts(it, period) })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    fun onPeriodSelected(period: AlertsPeriod) {
        selectedPeriod.value = period
    }

    /** A range picked in the date range picker, in either order. */
    fun onCustomRange(start: LocalDate, end: LocalDate) = onPeriodSelected(AlertsPeriod.Custom(minOf(start, end), maxOf(start, end)))

    /** "Mark as seasonal" toggles: a planned spike stops counting as one (and can be unmarked). */
    fun onToggleSeasonal(spike: SpikeAlert) {
        viewModelScope.launch { setSeasonal(spike.categoryId, spike.month, !spike.seasonal) }
    }
}
