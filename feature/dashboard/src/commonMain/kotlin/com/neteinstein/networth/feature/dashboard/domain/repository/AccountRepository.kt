package com.neteinstein.networth.feature.dashboard.domain.repository

import com.neteinstein.networth.feature.dashboard.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
}
