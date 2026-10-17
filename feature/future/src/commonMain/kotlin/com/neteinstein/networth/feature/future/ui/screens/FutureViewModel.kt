package com.neteinstein.networth.feature.future.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.future.domain.model.Future
import com.neteinstein.networth.feature.future.domain.model.FutureScope
import com.neteinstein.networth.feature.future.domain.model.WhatIfs
import com.neteinstein.networth.feature.future.domain.usecase.BuildForecastUseCase
import com.neteinstein.networth.feature.future.domain.usecase.BuildFutureUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class FutureUiState(
    val future: Future? = null,
    /** Years ahead the headline figures are for: 2, 5 or 10. */
    val horizon: Int = DEFAULT_HORIZON,
    /** The Year ahead card's "peaks in Dec, Jul and Sep". */
    val peakMonths: List<CalendarMonth> = emptyList(),
) {
    companion object {
        val HORIZONS = listOf(2, 5, 10)
        const val DEFAULT_HORIZON = 5
    }
}

private data class FutureChoices(
    val scope: FutureScope = FutureScope.ALL,
    val horizon: Int = FutureUiState.DEFAULT_HORIZON,
    val whatIfs: WhatIfs = WhatIfs(),
)

class FutureViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    private val buildFuture: BuildFutureUseCase,
    private val buildForecast: BuildForecastUseCase,
) : ViewModel() {
    private val choices = MutableStateFlow(FutureChoices())

    val state: StateFlow<FutureUiState> =
        combine(observeDatabase(), choices) { db, choice ->
            val data = (db as? DatabaseState.Ready)?.database ?: return@combine FutureUiState(horizon = choice.horizon)
            FutureUiState(
                future = buildFuture(data, choice.scope, choice.whatIfs),
                horizon = choice.horizon,
                peakMonths = buildForecast(data).peaks.map { it.month },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FutureUiState())

    fun onScopeSelected(scope: FutureScope) = choices.update { it.copy(scope = scope) }

    fun onHorizonSelected(years: Int) {
        if (years in FutureUiState.HORIZONS) choices.update { it.copy(horizon = years) }
    }

    fun onToggleCutDining() = choices.update { it.copy(whatIfs = it.whatIfs.copy(cutDining = !it.whatIfs.cutDining)) }

    fun onToggleExtra() = choices.update { it.copy(whatIfs = it.whatIfs.copy(addExtra = !it.whatIfs.addExtra)) }

    fun onToggleSetAside() = choices.update { it.copy(whatIfs = it.whatIfs.copy(setAside = !it.whatIfs.setAside)) }
}
