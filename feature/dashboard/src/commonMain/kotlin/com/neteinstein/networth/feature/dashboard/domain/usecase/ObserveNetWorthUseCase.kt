package com.neteinstein.networth.feature.dashboard.domain.usecase

import com.neteinstein.networth.feature.dashboard.domain.model.CurrencyCode
import com.neteinstein.networth.feature.dashboard.domain.model.NetWorthSummary
import com.neteinstein.networth.feature.dashboard.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Streams the net-worth summary, recomputed whenever accounts change. */
class ObserveNetWorthUseCase(
    private val repository: AccountRepository,
) {
    operator fun invoke(currency: CurrencyCode = CurrencyCode.DEFAULT): Flow<NetWorthSummary> =
        repository.observeAccounts().map { NetWorthSummary.from(it, currency) }
}
