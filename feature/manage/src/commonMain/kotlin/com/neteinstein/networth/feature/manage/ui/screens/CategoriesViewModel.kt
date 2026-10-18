package com.neteinstein.networth.feature.manage.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.format.MoneyParser
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.seed.DefaultCategories
import com.neteinstein.networth.core.domain.usecase.AddCategoryUseCase
import com.neteinstein.networth.core.domain.usecase.AddSubcategoryUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteCategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.MergeCategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.RemoveSubcategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.UpdateCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryItem(
    val category: Category,
    val ruleCount: Int,
    val transactionCount: Int,
)

data class CategoriesUiState(
    val loading: Boolean = true,
    val currencyCode: String = "EUR",
    val categories: List<CategoryItem> = emptyList(),
    val expandedId: String? = null,
    val palette: List<String> = DefaultCategories.palette,
)

class CategoriesViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    private val updateCategory: UpdateCategoryUseCase,
    private val addCategory: AddCategoryUseCase,
    private val addSubcategory: AddSubcategoryUseCase,
    private val removeSubcategory: RemoveSubcategoryUseCase,
    private val mergeCategory: MergeCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
) : ViewModel() {
    private val expanded = MutableStateFlow<String?>(null)

    val state: StateFlow<CategoriesUiState> = combine(observeDatabase(), expanded) { db, expanded ->
        val database = (db as? DatabaseState.Ready)?.database ?: return@combine CategoriesUiState()
        CategoriesUiState(
            loading = false,
            currencyCode = database.settings.currencyCode,
            categories = database.categories.sortedWith(compareBy({ it.position }, { it.name })).map { category ->
                CategoryItem(
                    category = category,
                    ruleCount = database.merchantRules.count { it.categoryId == category.id },
                    transactionCount = database.transactions.count { it.categoryId == category.id },
                )
            },
            expandedId = expanded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onToggle(categoryId: String) {
        expanded.value = if (expanded.value == categoryId) null else categoryId
    }

    fun onRename(categoryId: String, name: String) {
        if (name.isBlank()) return
        update(categoryId) { it.copy(name = name.trim()) }
    }

    fun onColorSelected(categoryId: String, colorHex: String) = update(categoryId) { it.copy(colorHex = colorHex) }

    /** Blank clears the budget; anything unparseable is ignored until it becomes a number. */
    fun onBudgetChange(categoryId: String, text: String) {
        val budget = if (text.isBlank()) null else MoneyParser.parseMinor(text)?.takeIf { it >= 0 } ?: return
        update(categoryId) { it.copy(monthlyBudgetMinor = budget) }
    }

    fun onSeasonalPeakChange(categoryId: String, month: Int?) = update(categoryId) { it.copy(seasonalPeakMonth = month) }

    fun onWarnOnSpikeChange(categoryId: String, warn: Boolean) = update(categoryId) { it.copy(warnOnSpike = warn) }

    fun onAddSubcategory(categoryId: String, name: String) {
        viewModelScope.launch { addSubcategory(categoryId, name) }
    }

    fun onRemoveSubcategory(categoryId: String, subcategoryId: String) {
        viewModelScope.launch { removeSubcategory(categoryId, subcategoryId) }
    }

    fun onAddCategory(name: String) {
        viewModelScope.launch { addCategory(name) }
    }

    fun onMerge(sourceId: String, targetId: String) {
        expanded.value = targetId
        viewModelScope.launch { mergeCategory(sourceId, targetId) }
    }

    fun onDelete(categoryId: String) {
        expanded.value = null
        viewModelScope.launch { deleteCategory(categoryId) }
    }

    private fun update(categoryId: String, change: (Category) -> Category) {
        viewModelScope.launch { updateCategory(categoryId, change) }
    }
}
