package com.neteinstein.networth.feature.spending.domain.repository

import com.neteinstein.networth.core.domain.model.TransactionFilter
import kotlinx.coroutines.flow.StateFlow

/** The filter being edited on the Filters screen, shared with the transaction list it opens. */
interface ActiveFilterRepository {
    val filter: StateFlow<TransactionFilter>

    fun set(filter: TransactionFilter)
}
