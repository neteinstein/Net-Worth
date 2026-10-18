package com.neteinstein.networth.feature.importer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.AddCategoryUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.importer.domain.ReviewQueue
import com.neteinstein.networth.feature.importer.domain.model.ReviewChoice
import com.neteinstein.networth.feature.importer.domain.model.ReviewItem
import com.neteinstein.networth.feature.importer.domain.usecase.SaveReviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the family picked on one item before saving. */
data class ReviewDraft(
    val categoryId: String? = null,
    val note: String = "",
    val remember: Boolean? = null,
)

data class ReviewItemUi(
    val item: ReviewItem,
    val draft: ReviewDraft,
) {
    /** The chip shown as chosen: a confirmed pick, else the suggestion (dashed). */
    val shownCategoryId: String? get() = draft.categoryId ?: item.suggestedCategoryId
    val remember: Boolean get() = draft.remember ?: (item.ruleKey != null)
}

data class ReviewUiState(
    val ready: Boolean = false,
    val items: List<ReviewItemUi> = emptyList(),
    val total: Int = 0,
    val categories: List<Category> = emptyList(),
    val knownMerchants: Int = 0,
    val currencyCode: String = "EUR",
    val saving: Boolean = false,
    /** Filed by the last save, including other transactions a learned rule caught. */
    val lastSaved: Int? = null,
) {
    val done: Int get() = items.count { it.draft.categoryId != null }
}

class ReviewViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    private val save: SaveReviewUseCase,
    private val addCategory: AddCategoryUseCase,
) : ViewModel() {
    private val drafts = MutableStateFlow<Map<String, ReviewDraft>>(emptyMap())
    private val status = MutableStateFlow(false to (null as Int?))

    val state: StateFlow<ReviewUiState> =
        combine(observeDatabase(), drafts, status) { db, picks, (saving, saved) ->
            val database = (db as? DatabaseState.Ready)?.database ?: return@combine ReviewUiState()
            val queue = ReviewQueue.build(database)
            ReviewUiState(
                ready = true,
                items = queue.take(PAGE).map { ReviewItemUi(it, picks[it.transaction.id] ?: ReviewDraft()) },
                total = queue.size,
                categories = database.categories.sortedBy { it.position },
                knownMerchants = ReviewQueue.knownMerchants(database),
                currencyCode = database.settings.currencyCode,
                saving = saving,
                lastSaved = saved,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    /** Picking the chosen category again clears it, so a wrong tap can be undone. */
    fun onPick(
        transactionId: String,
        categoryId: String,
    ) = edit(transactionId) { it.copy(categoryId = if (it.categoryId == categoryId) null else categoryId) }

    /** None of the categories fits: creates one and picks it for this movement. */
    fun onAddCategory(
        transactionId: String,
        name: String,
    ) {
        viewModelScope.launch {
            val category = addCategory(name) ?: return@launch
            edit(transactionId) { it.copy(categoryId = category.id) }
        }
    }

    fun onNote(
        transactionId: String,
        note: String,
    ) = edit(transactionId) { it.copy(note = note) }

    fun onRemember(
        transactionId: String,
        remember: Boolean,
    ) = edit(transactionId) { it.copy(remember = remember) }

    /** Confirms every suggestion that hasn't been picked yet. */
    fun onAcceptAll() {
        state.value.items.forEach { ui ->
            val suggestion = ui.item.suggestedCategoryId
            if (ui.draft.categoryId == null && suggestion != null) onPick(ui.item.transaction.id, suggestion)
        }
    }

    fun onSave() {
        val choices =
            state.value.items.mapNotNull { ui ->
                val category = ui.draft.categoryId ?: return@mapNotNull null
                ReviewChoice(ui.item.transaction.id, category, note = ui.draft.note, remember = ui.remember && ui.item.ruleKey != null)
            }
        if (choices.isEmpty()) return
        status.value = true to null
        viewModelScope.launch {
            val filed = save(choices)
            drafts.update { it - choices.map { c -> c.transactionId }.toSet() }
            status.value = false to filed
        }
    }

    private fun edit(
        transactionId: String,
        transform: (ReviewDraft) -> ReviewDraft,
    ) {
        drafts.update { it + (transactionId to transform(it[transactionId] ?: ReviewDraft())) }
        status.update { it.first to null }
    }

    private companion object {
        /** Items shown at once ("Showing 100 of 180"); the next batch appears after saving. */
        const val PAGE = 100
    }
}
