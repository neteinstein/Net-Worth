package com.neteinstein.networth.feature.spending.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.format.MoneyParser
import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.SavedFilter
import com.neteinstein.networth.core.domain.model.TransactionFilter
import com.neteinstein.networth.core.domain.model.TransactionTypeFilter
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.spending.domain.model.TransactionQuery
import com.neteinstein.networth.feature.spending.domain.model.adultIds
import com.neteinstein.networth.feature.spending.domain.model.find
import com.neteinstein.networth.feature.spending.domain.usecase.ObserveActiveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.SaveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.SetActiveFilterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The "Tagged for" chips: everyone, all adults, or one child. */
sealed interface TaggedFor {
    data object Everyone : TaggedFor

    data object Adults : TaggedFor

    data class Person(
        val memberId: String,
    ) : TaggedFor
}

data class FiltersUiState(
    val ready: Boolean = false,
    val filter: TransactionFilter = TransactionFilter(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val children: List<Member> = emptyList(),
    val tagged: Set<TaggedFor> = setOf(TaggedFor.Everyone),
    val minText: String = "",
    val maxText: String = "",
    val matchCount: Int = 0,
    val savedFilters: List<SavedFilter> = emptyList(),
    val currencyCode: String = "EUR",
) {
    val minValid: Boolean get() = minText.isBlank() || MoneyParser.parseMinor(minText) != null
    val maxValid: Boolean get() = maxText.isBlank() || MoneyParser.parseMinor(maxText) != null

    /** Empty account list means every account, so every chip shows as on. */
    fun isAccountSelected(id: String): Boolean = filter.accountIds.isEmpty() || id in filter.accountIds
}

class FiltersViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    observeFilter: ObserveActiveFilterUseCase,
    private val setFilter: SetActiveFilterUseCase,
    private val saveFilter: SaveFilterUseCase,
) : ViewModel() {
    private val amounts = MutableStateFlow("" to "")
    private val database = observeDatabase()
    private val active = observeFilter()

    val state: StateFlow<FiltersUiState> =
        combine(database, active, amounts) { db, filter, (minText, maxText) ->
            val data = (db as? DatabaseState.Ready)?.database ?: return@combine FiltersUiState(filter = filter)
            val adults = data.adultIds().toSet()
            val tagged: Set<TaggedFor> =
                when {
                    filter.memberIds.isEmpty() -> setOf(TaggedFor.Everyone)
                    else ->
                        buildSet<TaggedFor> {
                            if (adults.isNotEmpty() && filter.memberIds.containsAll(adults)) add(TaggedFor.Adults)
                            filter.memberIds.filter { it !in adults }.forEach { add(TaggedFor.Person(it)) }
                        }
                }
            FiltersUiState(
                ready = true,
                filter = filter,
                accounts = data.accounts.filter { !it.type.isLiability },
                categories = data.categories.sortedBy { it.position },
                children = data.members.filter { it.role == MemberRole.CHILD },
                tagged = tagged,
                minText = minText,
                maxText = maxText,
                matchCount = data.find(TransactionQuery(filter)).size,
                savedFilters = data.savedFilters.sortedBy { it.name },
                currencyCode = data.settings.currencyCode,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FiltersUiState())

    private fun update(change: (TransactionFilter) -> TransactionFilter) = setFilter(change(active.value))

    fun onAccountToggled(accountId: String) {
        val all = state.value.accounts.map { it.id }
        update { filter ->
            val selected = filter.accountIds.ifEmpty { all }
            val next = if (accountId in selected) selected - accountId else selected + accountId
            // Every account (or none) means no restriction.
            filter.copy(accountIds = if (next.size == all.size || next.isEmpty()) emptyList() else next)
        }
    }

    fun onCategoryToggled(categoryId: String) =
        update { it.copy(categoryIds = if (categoryId in it.categoryIds) it.categoryIds - categoryId else it.categoryIds + categoryId) }

    fun onTaggedToggled(choice: TaggedFor) {
        val db = (database.value as? DatabaseState.Ready)?.database ?: return
        val adults = db.adultIds()
        update { filter ->
            val ids =
                when (choice) {
                    TaggedFor.Everyone -> emptyList()
                    TaggedFor.Adults -> if (filter.memberIds.containsAll(adults)) {
                        filter.memberIds - adults.toSet()
                    } else {
                        (
                            filter.memberIds +
                                adults
                        ).distinct()
                    }
                    is TaggedFor.Person -> if (choice.memberId in
                        filter.memberIds
                    ) {
                        filter.memberIds - choice.memberId
                    } else {
                        filter.memberIds + choice.memberId
                    }
                }
            filter.copy(memberIds = ids)
        }
    }

    fun onTypeSelected(type: TransactionTypeFilter) = update { it.copy(type = type) }

    fun onIncludeTransfers(include: Boolean) = update { it.copy(includeTransfers = include) }

    fun onMinChanged(text: String) {
        amounts.value = text to amounts.value.second
        update { it.copy(minMinor = MoneyParser.parseMinor(text)?.let { v -> kotlin.math.abs(v) }) }
    }

    fun onMaxChanged(text: String) {
        amounts.value = amounts.value.first to text
        update { it.copy(maxMinor = MoneyParser.parseMinor(text)?.let { v -> kotlin.math.abs(v) }) }
    }

    fun onTextChanged(text: String) = update { it.copy(text = text.ifEmpty { null }) }

    fun onReset() {
        amounts.value = "" to ""
        setFilter(TransactionFilter())
    }

    fun onApplySaved(saved: SavedFilter) {
        amounts.value = (saved.filter.minMinor?.let(::plain) ?: "") to (saved.filter.maxMinor?.let(::plain) ?: "")
        setFilter(saved.filter)
    }

    fun onSave(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { saveFilter(name, active.value) }
    }

    private fun plain(minor: Long): String = "${minor / 100},${(minor % 100).toString().padStart(2, '0')}"
}
