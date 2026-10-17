package com.neteinstein.networth.feature.spending

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.TransactionFilter
import com.neteinstein.networth.core.domain.model.TransactionTypeFilter
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.FixedClock
import com.neteinstein.networth.core.testing.SequentialIds
import com.neteinstein.networth.feature.spending.data.InMemoryActiveFilterRepository
import com.neteinstein.networth.feature.spending.domain.model.PeriodKind
import com.neteinstein.networth.feature.spending.domain.model.UNCATEGORISED
import com.neteinstein.networth.feature.spending.domain.usecase.BuildSpendingUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.ObserveActiveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.SaveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.SetActiveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.UpdateTransactionUseCase
import com.neteinstein.networth.feature.spending.ui.screens.FiltersViewModel
import com.neteinstein.networth.feature.spending.ui.screens.SpendingViewModel
import com.neteinstein.networth.feature.spending.ui.screens.TaggedFor
import com.neteinstein.networth.feature.spending.ui.screens.TransactionsRequest
import com.neteinstein.networth.feature.spending.ui.screens.TransactionsViewModel
import com.neteinstein.networth.feature.spending.ui.screens.previewSpendingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpendingViewModelsTest {
    private val repository = FakeDatabaseRepository.ready(History.database())
    private val edit = EditDatabaseUseCase(repository)
    private val filters = InMemoryActiveFilterRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collecting(
        vm: T,
        state: (T) -> kotlinx.coroutines.flow.Flow<*>,
    ): T = vm.also { backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state(it).collect {} } }

    private fun TestScope.spending() =
        collecting(
            SpendingViewModel(
                ObserveDatabaseUseCase(repository),
                ObserveActiveFilterUseCase(filters),
                BuildSpendingUseCase(),
                FixedClock(LocalDate(2026, 10, 1)),
            ),
        ) { it.state }

    private fun TestScope.filtersVm() =
        collecting(
            FiltersViewModel(
                ObserveDatabaseUseCase(repository),
                ObserveActiveFilterUseCase(filters),
                SetActiveFilterUseCase(filters),
                SaveFilterUseCase(edit, SequentialIds("f")),
            ),
        ) { it.state }

    private fun TestScope.transactions() =
        collecting(
            TransactionsViewModel(ObserveDatabaseUseCase(repository), ObserveActiveFilterUseCase(filters), UpdateTransactionUseCase(edit)),
        ) {
            it.state
        }

    @Test
    fun spendingFollowsTheLatestMonthAndMovesBetweenPeriods() =
        runTest {
            val vm = spending()
            val initial = vm.state.value.spending!!
            assertEquals(LocalDate(2026, 9, 1), initial.period.start)
            assertEquals(PeriodKind.MONTH, initial.kind)

            vm.onPrevious()
            assertEquals(LocalDate(2026, 8, 1), vm.state.value.spending!!.period.start)
            vm.onNext()
            vm.onKindSelected(PeriodKind.WEEK)
            assertEquals(LocalDate(2026, 9, 28), vm.state.value.spending!!.period.start)
            vm.onKindSelected(PeriodKind.YEAR)
            assertEquals(LocalDate(2026, 1, 1), vm.state.value.spending!!.period.start)
            vm.onCustomRange(LocalDate(2026, 9, 10), LocalDate(2026, 9, 2))
            val custom = vm.state.value.spending!!
            assertEquals(PeriodKind.CUSTOM, custom.kind)
            assertEquals(LocalDate(2026, 9, 2), custom.period.start)
            assertEquals(9, custom.period.days)

            vm.onScopeSelected(AccountScope.Children)
            assertNotNull(vm.state.value.spending!!.savings)

            filters.set(TransactionFilter(text = "x"))
            assertEquals(1, vm.state.value.activeFilters)
            repository.state.value = DatabaseState.NotSelected
            assertNull(vm.state.value.spending)
            assertEquals(2, previewSpendingState().activeFilters)
        }

    @Test
    fun filtersEditTheSharedFilterAndCanBeSaved() =
        runTest {
            val vm = filtersVm()
            val db = repository.database
            assertTrue(vm.state.value.ready)
            // All-time expenses: 3 months × 2 of history plus September's 3.
            assertEquals(9, vm.state.value.matchCount)
            assertTrue(vm.state.value.isAccountSelected(History.pedro.id))

            vm.onAccountToggled(History.pedro.id)
            assertFalse(vm.state.value.isAccountSelected(History.pedro.id))
            assertEquals(4, vm.state.value.matchCount)
            vm.onAccountToggled(History.pedro.id)
            assertTrue(filters.filter.value.accountIds.isEmpty())

            vm.onCategoryToggled(History.groceries.id)
            assertEquals(listOf(History.groceries.id), filters.filter.value.categoryIds)
            vm.onCategoryToggled(History.groceries.id)

            val inês = db.members.first { it.name == "Inês" }.id
            vm.onTaggedToggled(TaggedFor.Person(inês))
            assertEquals(setOf<TaggedFor>(TaggedFor.Person(inês)), vm.state.value.tagged)
            assertEquals(1, vm.state.value.matchCount)
            vm.onTaggedToggled(TaggedFor.Adults)
            assertTrue(TaggedFor.Adults in vm.state.value.tagged)
            vm.onTaggedToggled(TaggedFor.Adults)
            vm.onTaggedToggled(TaggedFor.Person(inês))
            vm.onTaggedToggled(TaggedFor.Everyone)
            assertEquals(setOf<TaggedFor>(TaggedFor.Everyone), vm.state.value.tagged)

            vm.onTypeSelected(TransactionTypeFilter.BOTH)
            vm.onIncludeTransfers(true)
            vm.onMinChanged("100")
            vm.onMaxChanged("abc")
            assertFalse(vm.state.value.maxValid)
            assertTrue(vm.state.value.minValid)
            assertEquals(100_00, filters.filter.value.minMinor)
            vm.onTextChanged("pingo")
            assertEquals("pingo", filters.filter.value.text)

            vm.onSave("Groceries over 100")
            vm.onSave(" ")
            val saved = repository.database.savedFilters.single()
            assertEquals("Groceries over 100", saved.name)
            vm.onSave("GROCERIES OVER 100")
            assertEquals(1, repository.database.savedFilters.size)

            vm.onReset()
            assertEquals(TransactionFilter(), filters.filter.value)
            assertEquals("", vm.state.value.minText)
            vm.onApplySaved(saved)
            assertEquals("100,00", vm.state.value.minText)
            assertEquals(saved.filter, filters.filter.value)
            assertFailsWith<IllegalArgumentException> { SaveFilterUseCase(edit, SequentialIds()).invoke("", TransactionFilter()) }
        }

    @Test
    fun transactionsListACategoryOrTheFilterAndCanBeRefiled() =
        runTest {
            val vm = transactions()
            vm.open(TransactionsRequest(categoryId = History.school.id, start = LocalDate(2026, 9, 1), end = LocalDate(2026, 9, 30)))
            assertEquals("School", vm.state.value.category?.name)
            assertEquals(1, vm.state.value.lines.size)
            assertEquals(-250_00, vm.state.value.totalMinor)

            vm.open(TransactionsRequest(categoryId = UNCATEGORISED, accountIds = listOf(History.pedro.id)))
            assertNull(vm.state.value.category)
            val loja = vm.state.value.lines.single().transaction
            assertEquals("Pedro", vm.state.value.lines.single().accountName)

            vm.onEdit(loja.id)
            val inês = repository.database.members.first { it.name == "Inês" }.id
            val sub = History.groceries.subcategories.first().id
            vm.onEditorChange {
                it.copy(categoryId = History.groceries.id, subcategoryId = sub, memberIds = listOf(inês), note = "  bread ")
            }
            vm.onSaveEdit()
            assertNull(vm.state.value.editor)
            val saved = repository.database.transactions.first { it.id == loja.id }
            assertEquals(History.groceries.id, saved.categoryId)
            assertEquals(sub, saved.subcategoryId)
            assertEquals(listOf(inês), saved.memberIds)
            assertEquals("bread", saved.note)
            assertTrue(vm.state.value.lines.isEmpty())

            filters.set(TransactionFilter(text = "pingo"))
            vm.open(TransactionsRequest(fromFilters = true))
            assertEquals(4, vm.state.value.lines.size)
            val pingo = vm.state.value.lines.first().transaction
            vm.onEdit(pingo.id)
            vm.onEditorChange { it.copy(isInternalTransfer = true) }
            vm.onSaveEdit()
            val transfer = repository.database.transactions.first { it.id == pingo.id }
            assertTrue(transfer.isInternalTransfer)
            assertNull(transfer.categoryId)

            vm.onEdit("missing")
            assertNull(vm.state.value.editor)
            vm.onEdit(vm.state.value.lines.first().transaction.id)
            vm.onDismissEditor()
            assertNull(vm.state.value.editor)
            vm.onSaveEdit()
        }
}
