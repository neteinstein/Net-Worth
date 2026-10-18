package com.neteinstein.networth.feature.manage.domain.usecase

import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.model.withTombstone
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase

/** Applies [change] to one category, stamping it as edited now. */
class UpdateCategoryUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(categoryId: String, change: (Category) -> Category) = edit { database, now ->
        val category = database.category(categoryId) ?: return@edit database
        database.copy(categories = database.categories.upsert(change(category).copy(updatedAt = now)))
    }
}

/** Removes a subcategory; its transactions stay in the parent category. */
class RemoveSubcategoryUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(categoryId: String, subcategoryId: String) = edit { database, now ->
        val category = database.category(categoryId) ?: return@edit database
        database.copy(
            categories = database.categories.upsert(
                category.copy(subcategories = category.subcategories.filterNot { it.id == subcategoryId }, updatedAt = now),
            ),
        ).refile(now, { it.subcategoryId == subcategoryId }) { it.copy(subcategoryId = null) }
    }
}

/**
 * Moves everything from [sourceId] into [targetId] — transactions, merchant rules and
 * subcategories — and removes the source, so past months stay comparable.
 */
class MergeCategoryUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(sourceId: String, targetId: String) = edit { database, now ->
        if (sourceId == targetId) return@edit database
        val source = database.category(sourceId) ?: return@edit database
        val target = database.category(targetId) ?: return@edit database
        val merged = target.copy(
            subcategories = target.subcategories + source.subcategories.filter { sub -> target.subcategories.none { it.name == sub.name } },
            updatedAt = now,
        )
        database.withTombstone(sourceId, now).copy(
            categories = database.categories.filterNot { it.id == sourceId }.upsert(merged),
            merchantRules = database.merchantRules.map {
                if (it.categoryId ==
                    sourceId
                ) {
                    it.copy(categoryId = targetId, updatedAt = now)
                } else {
                    it
                }
            },
        ).refile(now, { it.categoryId == sourceId }) { tx ->
            val subName = source.subcategories.firstOrNull { it.id == tx.subcategoryId }?.name
            tx.copy(categoryId = targetId, subcategoryId = merged.subcategories.firstOrNull { it.name == subName }?.id ?: tx.subcategoryId)
        }
    }
}

/** Deletes a category: its transactions become uncategorised and its rules are removed. */
class DeleteCategoryUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(categoryId: String) = edit { database, now ->
        val rules = database.merchantRules.filter { it.categoryId == categoryId }
        rules.fold(database.withTombstone(categoryId, now)) { db, rule -> db.withTombstone(rule.id, now) }.copy(
            categories = database.categories.filterNot { it.id == categoryId },
            merchantRules = database.merchantRules - rules.toSet(),
        ).refile(now, { it.categoryId == categoryId }) { it.copy(categoryId = null, subcategoryId = null) }
    }
}

private fun FamilyDatabase.refile(
    now: Long,
    matches: (com.neteinstein.networth.core.domain.model.Transaction) -> Boolean,
    change: (com.neteinstein.networth.core.domain.model.Transaction) -> com.neteinstein.networth.core.domain.model.Transaction,
): FamilyDatabase = copy(transactions = transactions.map { if (matches(it)) change(it).copy(updatedAt = now) else it })
