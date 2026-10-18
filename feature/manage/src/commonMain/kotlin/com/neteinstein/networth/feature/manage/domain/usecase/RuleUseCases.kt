package com.neteinstein.networth.feature.manage.domain.usecase

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.text.Descriptions
import com.neteinstein.networth.core.domain.text.MovementKind
import com.neteinstein.networth.core.domain.text.Rules
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.feature.manage.domain.RuleFile
import com.neteinstein.networth.feature.manage.domain.RuleFileEntry

/** The transactions a merchant rule files: spending and refunds, never transfers between the family's accounts. */
fun FamilyDatabase.transactionsMatching(rule: MerchantRule): List<Transaction> = transactions.filter { tx ->
    if (tx.isInternalTransfer) return@filter false
    val info = Descriptions.describe(tx.description)
    (tx.amountMinor < 0 || info.kind == MovementKind.REFUND) && Rules.matches(rule, tx.description, info)
}

/**
 * The transactions [rules] would file somewhere else than they are today, each with the most specific of
 * [rules] that matches it (as an import would pick it).
 */
fun FamilyDatabase.refiledBy(rules: List<MerchantRule>): Map<Transaction, MerchantRule> {
    if (rules.isEmpty()) return emptyMap()
    return transactions.mapNotNull { tx ->
        if (tx.isInternalTransfer) return@mapNotNull null
        val info = Descriptions.describe(tx.description)
        if (tx.amountMinor >= 0 && info.kind != MovementKind.REFUND) return@mapNotNull null
        val rule = Rules.find(rules, tx.description, info) ?: return@mapNotNull null
        (tx to rule).takeIf { tx.categoryId != rule.categoryId || tx.subcategoryId != rule.subcategoryId }
    }.toMap()
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

/**
 * Imports a merchant rules file: adds its new rules and changes the category of rules the household already has
 * (same text and match). Rules whose category the household doesn't have are left out. With [applyToPast] every
 * earlier transaction the file's rules match is re-filed. Returns how many transactions were re-filed.
 */
class ImportMerchantRulesUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(entries: List<RuleFileEntry>, applyToPast: Boolean): Int {
        var refiled = 0
        edit { database, now ->
            val plan = RuleFile.plan(entries, database.categories, database.merchantRules)
            val added = plan.toAdd.map { MerchantRule(ids.newId(), it.pattern, it.match, it.categoryId, it.subcategoryId, updatedAt = now) }
            val rules = (
                plan.toUpdate.map {
                    it.copy(updatedAt = now)
                } + added
            ).fold(database.merchantRules) { all, rule -> all.upsert(rule) }
            val moves = if (applyToPast) database.refiledBy(plan.rules).mapKeys { it.key.id } else emptyMap()
            refiled = moves.size
            database.copy(
                merchantRules = rules,
                transactions = database.transactions.map { tx ->
                    val rule = moves[tx.id] ?: return@map tx
                    tx.copy(categoryId = rule.categoryId, subcategoryId = rule.subcategoryId, updatedAt = now)
                },
            )
        }
        return refiled
    }
}
