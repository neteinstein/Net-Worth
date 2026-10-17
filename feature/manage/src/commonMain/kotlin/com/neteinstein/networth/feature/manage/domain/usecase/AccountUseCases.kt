package com.neteinstein.networth.feature.manage.domain.usecase

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.BalanceSnapshot
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.model.withTombstone
import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase

/** What the account editor submits. [id] is null for a new account. */
data class AccountDraft(
    val id: String?,
    val name: String,
    val type: AccountType,
    val ownerIds: List<String>,
    val bank: String?,
    val accountNumber: String?,
    val iban: String?,
    /** When set, records this as the balance today (a manual balance snapshot). */
    val balanceMinor: Long?,
)

class SaveAccountUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
    private val clock: AppClock,
) {
    suspend operator fun invoke(draft: AccountDraft) {
        require(draft.name.isNotBlank()) { "Account name is required" }
        val accountId = draft.id ?: ids.newId()
        val today = clock.today()
        edit { database, now ->
            val account = Account(
                id = accountId,
                name = draft.name.trim(),
                type = draft.type,
                ownerIds = draft.ownerIds,
                bank = draft.bank?.trim()?.takeIf { it.isNotEmpty() },
                accountNumber = draft.accountNumber?.trim()?.takeIf { it.isNotEmpty() },
                iban = draft.iban?.filterNot { it.isWhitespace() }?.uppercase()?.takeIf { it.isNotEmpty() },
                updatedAt = now,
            )
            val snapshots = draft.balanceMinor?.let { amount ->
                // One manual snapshot per account per day: typing a new balance replaces today's.
                val existing = database.balanceSnapshots.firstOrNull { it.accountId == accountId && it.date == today }
                database.balanceSnapshots.upsert(BalanceSnapshot(existing?.id ?: ids.newId(), accountId, today, amount, now))
            } ?: database.balanceSnapshots
            database.copy(accounts = database.accounts.upsert(account), balanceSnapshots = snapshots)
        }
    }
}

/** Removes an account that has no transactions (so history is never silently lost). */
class DeleteAccountUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(accountId: String) = edit { database, now ->
        if (database.transactions.any { it.accountId == accountId }) return@edit database
        val snapshots = database.balanceSnapshots.filter { it.accountId == accountId }
        snapshots.fold(database.withTombstone(accountId, now)) { db, s -> db.withTombstone(s.id, now) }.copy(
            accounts = database.accounts.filterNot { it.id == accountId },
            balanceSnapshots = database.balanceSnapshots - snapshots.toSet(),
        )
    }
}

class SetAskAccountOnImportUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(ask: Boolean) = edit { database, now ->
        database.copy(settings = database.settings.copy(askAccountOnImport = ask, updatedAt = now))
    }
}

class SaveAlertSettingsUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(spikeThresholdPct: Int, largePaymentMinor: Long, newSubscriptions: Boolean) = edit { database, now ->
        val alerts = database.settings.alerts.copy(
            spikeThresholdPct = spikeThresholdPct.coerceIn(10, 200),
            largePaymentMinor = largePaymentMinor.coerceAtLeast(0),
            newSubscriptions = newSubscriptions,
        )
        database.copy(settings = database.settings.copy(alerts = alerts, updatedAt = now))
    }
}

class DeleteMerchantRuleUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(ruleId: String) = edit { database, now ->
        database.withTombstone(ruleId, now).copy(merchantRules = database.merchantRules.filterNot { it.id == ruleId })
    }
}
