package com.neteinstein.networth.feature.spending.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.latestMonth
import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.spending.domain.model.DateRange
import com.neteinstein.networth.feature.spending.domain.model.PeriodKind
import com.neteinstein.networth.feature.spending.domain.model.activeCount
import com.neteinstein.networth.feature.spending.domain.usecase.BuildSpendingUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.ObserveActiveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.Spending
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

data class SpendingUiState(
    val spending: Spending? = null,
    val activeFilters: Int = 0,
)

private data class SpendingSelection(
    val kind: PeriodKind = PeriodKind.MONTH,
    /** Null until the user moves: follow the latest month with data. */
    val period: DateRange? = null,
    val scope: AccountScope = AccountScope.All,
)

class SpendingViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    observeFilter: ObserveActiveFilterUseCase,
    private val buildSpending: BuildSpendingUseCase,
    private val clock: AppClock,
) : ViewModel() {
    private val selection = MutableStateFlow(SpendingSelection())

    val state: StateFlow<SpendingUiState> =
        combine(observeDatabase(), selection, observeFilter()) { db, sel, filter ->
            val database = (db as? DatabaseState.Ready)?.database ?: return@combine SpendingUiState(activeFilters = filter.activeCount)
            val period = sel.period ?: DateRange.of(sel.kind, (database.latestMonth()?.firstDay ?: clock.today()))
            SpendingUiState(buildSpending(database, sel.kind, period, sel.scope), filter.activeCount)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpendingUiState())

    private fun current(): DateRange? = state.value.spending?.period

    fun onKindSelected(kind: PeriodKind) {
        val anchor = current()?.end ?: clock.today()
        selection.update { it.copy(kind = kind, period = DateRange.of(kind, anchor)) }
    }

    /** A custom range picked in the date range picker. */
    fun onCustomRange(
        start: LocalDate,
        end: LocalDate,
    ) {
        selection.update { it.copy(kind = PeriodKind.CUSTOM, period = DateRange(minOf(start, end), maxOf(start, end))) }
    }

    fun onPrevious() = move(-1)

    fun onNext() = move(1)

    private fun move(steps: Int) {
        val period = current() ?: return
        selection.update { it.copy(period = period.shift(it.kind, steps)) }
    }

    fun onScopeSelected(scope: AccountScope) {
        selection.update { it.copy(scope = scope) }
    }
}
