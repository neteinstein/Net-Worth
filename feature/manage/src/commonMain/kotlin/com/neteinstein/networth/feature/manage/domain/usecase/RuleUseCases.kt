package com.neteinstein.networth.feature.manage.domain.usecase

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.text.Descriptions
import com.neteinstein.networth.core.domain.text.MovementKind
import com.neteinstein.networth.core.domain.text.Rules
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase

/** The transactions a merchant rule files: spending and refunds, never transfers between the family's accounts. */
fun FamilyDatabase.transactionsMatching(rule: MerchantRule): List<Transaction> = transactions.filter { tx ->
    if (tx.isInternalTransfer) return@filter false
    val info = Descriptions.describe(tx.description)
    (tx.amountMinor < 0 || info.kind == MovementKind.REFUND) && Rules.matches(rule, tx.description, info)
}

/**
 * Saves a changed merchant rule. With [applyToPast] every earlier transaction it matches is re-filed under
 * its category (overriding what was chosen before); otherwise only future imports use it.
 * Returns how many transactions were re-filed.
 */
class SaveMerchantRuleUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(rule: MerchantRule, applyToPast: Boolean): Int {
        var refiled = 0
        edit { database, now ->
            refiled = 0
            val saved = rule.copy(pattern = rule.pattern.trim(), updatedAt = now)
            val matching = if (applyToPast) database.transactionsMatching(saved).map { it.id }.toSet() else emptySet()
            database.copy(
                merchantRules = database.merchantRules.upsert(saved),
                transactions = database.transactions.map { tx ->
                    if (tx.id !in matching) return@map tx
                    if (tx.categoryId == saved.categoryId && tx.subcategoryId == saved.subcategoryId) return@map tx
                    refiled++
                    tx.copy(categoryId = saved.categoryId, subcategoryId = saved.subcategoryId, updatedAt = now)
                },
            )
        }
        return refiled
    }
}
