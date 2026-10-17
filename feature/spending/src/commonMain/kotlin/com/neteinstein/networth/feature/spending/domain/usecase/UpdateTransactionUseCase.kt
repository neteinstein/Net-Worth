package com.neteinstein.networth.feature.spending.domain.usecase

import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase

/** What can be changed on one transaction from the list. */
data class TransactionEdit(
    val categoryId: String?,
    val subcategoryId: String?,
    /** "Tagged for": who in the family the money was for. */
    val memberIds: List<String>,
    val note: String?,
    val isInternalTransfer: Boolean,
)

/** Re-files a transaction. Marking it as an internal transfer removes its category: transfers are never spending. */
class UpdateTransactionUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(transactionId: String, change: TransactionEdit) = edit { database, now ->
        database.copy(
            transactions = database.transactions.map { tx ->
                if (tx.id != transactionId) return@map tx
                val transfer = change.isInternalTransfer
                tx.copy(
                    categoryId = if (transfer) null else change.categoryId,
                    subcategoryId = if (transfer || change.categoryId == null) null else change.subcategoryId,
                    memberIds = change.memberIds,
                    note = change.note?.trim()?.takeIf { it.isNotEmpty() },
                    isInternalTransfer = transfer,
                    updatedAt = now,
                )
            },
        )
    }
}
