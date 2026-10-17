package com.neteinstein.networth.feature.future.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.future.domain.model.Forecast
import com.neteinstein.networth.feature.future.domain.usecase.BuildForecastUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class YearAheadUiState(
    val forecast: Forecast? = null,
    /** Chip labels for single accounts, by account id. */
    val accountNames: Map<String, String> = emptyMap(),
)

class YearAheadViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    private val buildForecast: BuildForecastUseCase,
) : ViewModel() {
    private val scope = MutableStateFlow<AccountScope>(AccountScope.All)

    val state: StateFlow<YearAheadUiState> =
        combine(observeDatabase(), scope) { db, selected ->
            val data: FamilyDatabase = (db as? DatabaseState.Ready)?.database ?: return@combine YearAheadUiState()
            YearAheadUiState(buildForecast(data, selected), data.accounts.associate { it.id to it.name })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearAheadUiState())

    fun onScopeSelected(selected: AccountScope) {
        scope.value = selected
    }
}
