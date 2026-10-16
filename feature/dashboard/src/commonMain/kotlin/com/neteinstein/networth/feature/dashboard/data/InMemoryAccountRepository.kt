package com.neteinstein.networth.feature.dashboard.data

import com.neteinstein.networth.feature.dashboard.domain.model.Account
import com.neteinstein.networth.feature.dashboard.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Placeholder data source until the real one (persistence / sync) is specified.
 * Seeded with [SampleAccounts] so the UI has something to render.
 */
class InMemoryAccountRepository(
    seed: List<Account> = SampleAccounts.all,
) : AccountRepository {
    private val accounts = MutableStateFlow(seed)

    override fun observeAccounts(): Flow<List<Account>> = accounts.asStateFlow()

    fun upsert(account: Account) {
        accounts.update { current -> current.filterNot { it.id == account.id } + account }
    }

    fun remove(id: String) {
        accounts.update { current -> current.filterNot { it.id == id } }
    }
}
