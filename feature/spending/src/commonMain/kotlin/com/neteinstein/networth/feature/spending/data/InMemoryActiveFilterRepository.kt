package com.neteinstein.networth.feature.spending.data

import com.neteinstein.networth.core.domain.model.TransactionFilter
import com.neteinstein.networth.feature.spending.domain.repository.ActiveFilterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Kept for the session only: filters worth keeping are saved to the family database as SavedFilters. */
class InMemoryActiveFilterRepository : ActiveFilterRepository {
    private val state = MutableStateFlow(TransactionFilter())

    override val filter: StateFlow<TransactionFilter> = state

    override fun set(filter: TransactionFilter) {
        state.value = filter
    }
}
