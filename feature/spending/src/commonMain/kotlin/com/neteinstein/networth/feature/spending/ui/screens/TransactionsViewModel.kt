package com.neteinstein.networth.feature.spending.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.model.TransactionFilter
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.AddCategoryUseCase
import com.neteinstein.networth.core.domain.usecase.AddSubcategoryUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.spending.domain.model.DateRange
import com.neteinstein.networth.feature.spending.domain.model.TransactionQuery
import com.neteinstein.networth.feature.spending.domain.model.UNCATEGORISED
import com.neteinstein.networth.feature.spending.domain.model.find
import com.neteinstein.networth.feature.spending.domain.usecase.ObserveActiveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.TransactionEdit
import com.neteinstein.networth.feature.spending.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * What a transaction list shows. From Filters it follows the active filter; from a Spending row or
 * an alert it is one category (or "Not categorised yet") over a period and set of accounts.
 */
data class TransactionsRequest(
    val categoryId: String? = null,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val accountIds: List<String> = emptyList(),
    val fromFilters: Boolean = false,
)

data class TransactionLine(
    val transaction: Transaction,
    val accountName: String,
    val category: Category?,
)

/** The edit sheet for one transaction. */
data class TransactionEditor(
    val transactionId: String,
    val description: String,
    val amountMinor: Long,
    val edit: TransactionEdit,
)

data class TransactionsUiState(
    val ready: Boolean = false,
    val request: TransactionsRequest = TransactionsRequest(),
    /** The category the list is about, for its title. */
    val category: Category? = null,
    val lines: List<TransactionLine> = emptyList(),
    val totalMinor: Long = 0,
    val categories: List<Category> = emptyList(),
    val members: List<Member> = emptyList(),
    val editor: TransactionEditor? = null,
    val currencyCode: String = "EUR",
)

class TransactionsViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    observeFilter: ObserveActiveFilterUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val addCategory: AddCategoryUseCase,
    private val addSubcategory: AddSubcategoryUseCase,
) : ViewModel() {
    private val request = MutableStateFlow(TransactionsRequest())
    private val editor = MutableStateFlow<TransactionEditor?>(null)

    val state: StateFlow<TransactionsUiState> =
        combine(observeDatabase(), observeFilter(), request, editor) { db, active, req, edit ->
            val database = (db as? DatabaseState.Ready)?.database ?: return@combine TransactionsUiState(request = req)
            val filter =
                if (req.fromFilters) {
                    active
                } else {
                    TransactionFilter(accountIds = req.accountIds, categoryIds = listOfNotNull(req.categoryId))
                }
            val period = if (req.start != null && req.end != null) DateRange(req.start, req.end) else null
            val found = database.find(TransactionQuery(filter, period))
            val names = database.accounts.associate { it.id to it.name }
            TransactionsUiState(
                ready = true,
                request = req,
                category = req.categoryId?.takeIf { it != UNCATEGORISED }?.let { database.category(it) },
                lines = found.map { TransactionLine(it, names[it.accountId].orEmpty(), database.category(it.categoryId)) },
                totalMinor = found.sumOf { it.amountMinor },
                categories = database.categories.sortedBy { it.position },
                members = database.members,
                editor = edit,
                currencyCode = database.settings.currencyCode,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    /** Called when the screen opens (ViewModels are shared by every list on the back stack). */
    fun open(next: TransactionsRequest) {
        request.value = next
        editor.value = null
    }

    fun onEdit(transactionId: String) {
        val tx = state.value.lines.firstOrNull { it.transaction.id == transactionId }?.transaction ?: return
        editor.value =
            TransactionEditor(
                transactionId = tx.id,
                description = tx.description,
                amountMinor = tx.amountMinor,
                edit = TransactionEdit(tx.categoryId, tx.subcategoryId, tx.memberIds, tx.note, tx.isInternalTransfer),
            )
    }

    fun onEditorChange(change: (TransactionEdit) -> TransactionEdit) = editor.update { it?.copy(edit = change(it.edit)) }

    /** None of the categories fits: creates one and files the edited transaction under it. */
    fun onAddCategory(name: String) {
        if (editor.value == null) return
        viewModelScope.launch {
            val category = addCategory(name) ?: return@launch
            onEditorChange { it.copy(categoryId = category.id, subcategoryId = null) }
        }
    }

    /** Creates a subcategory of the edited transaction's category and picks it. */
    fun onAddSubcategory(name: String) {
        val categoryId = editor.value?.edit?.categoryId ?: return
        viewModelScope.launch {
            val sub = addSubcategory(categoryId, name) ?: return@launch
            onEditorChange { if (it.categoryId == categoryId) it.copy(subcategoryId = sub.id) else it }
        }
    }

    fun onDismissEditor() {
        editor.value = null
    }

    fun onSaveEdit() {
        val current = editor.value ?: return
        editor.value = null
        viewModelScope.launch { updateTransaction(current.transactionId, current.edit) }
    }
}
