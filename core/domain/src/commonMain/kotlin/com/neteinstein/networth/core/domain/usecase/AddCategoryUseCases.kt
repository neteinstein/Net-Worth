package com.neteinstein.networth.core.domain.usecase

import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.Subcategory
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.seed.DefaultCategories

/**
 * Adds a category, last in the list with the next palette colour, from Categories or from wherever a
 * transaction is filed (when none fits), so it can be picked straight away. Returns it, or null for a blank name.
 */
class AddCategoryUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(name: String): Category? {
        if (name.isBlank()) return null
        val id = ids.newId()
        var added: Category? = null
        edit { database, now ->
            val palette = DefaultCategories.palette
            val category = Category(
                id = id,
                name = name.trim(),
                colorHex = palette[database.categories.size % palette.size],
                position = (database.categories.maxOfOrNull { it.position } ?: -1) + 1,
                updatedAt = now,
            )
            added = category
            database.copy(categories = database.categories + category)
        }
        return added
    }
}

/** Adds a subcategory to a category (see [AddCategoryUseCase]). Returns it, or null for a blank name or unknown category. */
class AddSubcategoryUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(categoryId: String, name: String): Subcategory? {
        if (name.isBlank()) return null
        val sub = Subcategory(ids.newId(), name.trim())
        var added: Subcategory? = null
        edit { database, now ->
            val category = database.category(categoryId) ?: return@edit database
            added = sub
            database.copy(
                categories = database.categories.upsert(category.copy(subcategories = category.subcategories + sub, updatedAt = now)),
            )
        }
        return added
    }
}
