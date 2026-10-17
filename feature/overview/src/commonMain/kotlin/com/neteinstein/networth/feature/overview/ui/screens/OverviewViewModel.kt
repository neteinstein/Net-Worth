package com.neteinstein.networth.feature.overview.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.latestMonth
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.SyncDatabaseUseCase
import com.neteinstein.networth.feature.overview.domain.usecase.BuildOverviewUseCase
import com.neteinstein.networth.feature.overview.domain.usecase.MarkSeasonalUseCase
import com.neteinstein.networth.feature.overview.domain.usecase.Overview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OverviewUiState(
    val overview: Overview? = null,
    val sync: SyncStatus? = null,
)

class OverviewViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    private val buildOverview: BuildOverviewUseCase,
    private val markSeasonal: MarkSeasonalUseCase,
    private val syncDatabase: SyncDatabaseUseCase,
    private val clock: AppClock,
) : ViewModel() {
    /** Null until the user picks one: follow the latest month that has data. */
    private val selectedMonth = MutableStateFlow<CalendarMonth?>(null)
    private val selectedScope = MutableStateFlow<AccountScope>(AccountScope.All)

    val state: StateFlow<OverviewUiState> = combine(observeDatabase(), selectedMonth, selectedScope) { db, month, scope ->
        val ready = db as? DatabaseState.Ready ?: return@combine OverviewUiState()
        OverviewUiState(buildOverview(ready.database, month ?: defaultMonth(ready.database), scope), ready.sync)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    fun onMonthSelected(month: CalendarMonth) {
        selectedMonth.value = month
    }

    fun onScopeSelected(scope: AccountScope) {
        selectedScope.value = scope
    }

    fun onMarkSeasonal(categoryId: String) {
        val month = state.value.overview?.month ?: return
        viewModelScope.launch { markSeasonal(categoryId, month) }
    }

    fun onSyncNow() {
        viewModelScope.launch { syncDatabase() }
    }

    private fun defaultMonth(database: FamilyDatabase): CalendarMonth = database.latestMonth() ?: CalendarMonth.of(clock.today())
}
