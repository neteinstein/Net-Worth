package com.neteinstein.networth.feature.spending.domain.usecase

import com.neteinstein.networth.core.domain.model.SavedFilter
import com.neteinstein.networth.core.domain.model.TransactionFilter
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.feature.spending.domain.repository.ActiveFilterRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveActiveFilterUseCase(private val repository: ActiveFilterRepository) {
    operator fun invoke(): StateFlow<TransactionFilter> = repository.filter
}

class SetActiveFilterUseCase(private val repository: ActiveFilterRepository) {
    operator fun invoke(filter: TransactionFilter) = repository.set(filter)
}

/** "Save filter": stores the current filter under [name] for the whole family. Same name replaces it. */
class SaveFilterUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(name: String, filter: TransactionFilter) {
        require(name.isNotBlank()) { "Name is required" }
        edit { database, now ->
            val existing = database.savedFilters.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            database.copy(savedFilters = database.savedFilters.upsert(SavedFilter(existing?.id ?: ids.newId(), name.trim(), filter, now)))
        }
    }
}
