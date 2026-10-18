package com.neteinstein.networth.feature.manage

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.RuleMatch
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.SequentialIds
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.manage.domain.usecase.AddCategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.AddSubcategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteCategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.MergeCategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.RemoveSubcategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.UpdateCategoryUseCase
import com.neteinstein.networth.feature.manage.ui.screens.CategoriesViewModel
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesTest {
    private lateinit var school: String
    private lateinit var kids: String
    private lateinit var books: String

    private fun family(): FamilyDatabase {
        val base = designFamily()
        val schoolCategory = base.categories.first { it.name == "School" }
        school = schoolCategory.id
        kids = base.categories.first { it.name == "Kids’ activities" }.id
        books = schoolCategory.subcategories.first().id
        val account = base.accounts.first().id
        return base.copy(
            transactions = listOf(
                Transaction("t1", account, LocalDate(2026, 9, 2), -45_00, "ASSOC PAIS", categoryId = school, subcategoryId = books),
                Transaction("t2", account, LocalDate(2026, 9, 3), -20_00, "DECATHLON", categoryId = kids),
            ),
            merchantRules = listOf(MerchantRule("r", "ASSOC PAIS", RuleMatch.PREFIX, school)),
        )
    }

    private val repository = FakeDatabaseRepository.ready(family())
    private val edit = EditDatabaseUseCase(repository)
    private val update = UpdateCategoryUseCase(edit)
    private val ids = SequentialIds("c")

    private fun TestScope.viewModel() = CategoriesViewModel(
        observeDatabase = ObserveDatabaseUseCase(repository),
        updateCategory = update,
        addCategory = AddCategoryUseCase(edit, ids),
        addSubcategory = AddSubcategoryUseCase(update, ids),
        removeSubcategory = RemoveSubcategoryUseCase(edit),
        mergeCategory = MergeCategoryUseCase(edit),
        deleteCategory = DeleteCategoryUseCase(edit),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun listsCategoriesInOrderWithCounts() = runTest {
        val vm = viewModel()
        val state = vm.state.value

        assertEquals("Housing", state.categories.first().category.name)
        val item = state.categories.first { it.category.id == school }
        assertEquals(1, item.ruleCount)
        assertEquals(1, item.transactionCount)

        vm.onToggle(school)
        assertEquals(school, vm.state.value.expandedId)
        vm.onToggle(school)
        assertNull(vm.state.value.expandedId)
    }

    @Test
    fun editsCategoryFields() = runTest {
        val vm = viewModel()

        vm.onRename(school, "  Escola ")
        vm.onRename(school, " ")
        vm.onColorSelected(school, "#2D55D6")
        vm.onBudgetChange(school, "300")
        vm.onBudgetChange(school, "abc")
        vm.onSeasonalPeakChange(school, 9)
        vm.onWarnOnSpikeChange(school, false)

        val category = repository.database.category(school)!!
        assertEquals("Escola", category.name)
        assertEquals("#2D55D6", category.colorHex)
        assertEquals(300_00, category.monthlyBudgetMinor)
        assertEquals(9, category.seasonalPeakMonth)
        assertEquals(false, category.warnOnSpike)

        vm.onBudgetChange(school, "")
        assertNull(repository.database.category(school)!!.monthlyBudgetMinor)
        vm.onRename("missing", "x")
    }

    @Test
    fun subcategoriesAndNewCategories() = runTest {
        val vm = viewModel()

        vm.onAddSubcategory(school, "Uniforms")
        vm.onAddSubcategory(school, " ")
        vm.onRemoveSubcategory(school, books)
        vm.onAddCategory("Pets")
        vm.onAddCategory("")

        val category = repository.database.category(school)!!
        assertEquals("Uniforms", category.subcategories.last().name)
        assertTrue(category.subcategories.none { it.id == books })
        assertNull(repository.database.transactions.first { it.id == "t1" }.subcategoryId)
        val pets = repository.database.categories.last()
        assertEquals("Pets", pets.name)
        assertEquals(11, pets.position)
        assertEquals(12, repository.database.categories.size)
    }

    @Test
    fun mergeMovesTransactionsRulesAndSubcategories() = runTest {
        val vm = viewModel()

        vm.onMerge(school, kids)
        vm.onMerge(kids, kids)

        val db = repository.database
        assertNull(db.category(school))
        assertTrue(school in db.tombstones)
        val target = db.category(kids)!!
        assertTrue(target.subcategories.any { it.name == "Books & materials" })
        val moved = db.transactions.first { it.id == "t1" }
        assertEquals(kids, moved.categoryId)
        assertEquals(target.subcategories.first { it.name == "Books & materials" }.id, moved.subcategoryId)
        assertEquals(kids, db.merchantRules.single().categoryId)
        assertEquals(kids, vm.state.value.expandedId)
    }

    @Test
    fun deleteUncategorisesAndDropsRules() = runTest {
        val vm = viewModel()

        vm.onDelete(school)

        val db = repository.database
        assertNull(db.category(school))
        assertNull(db.transactions.first { it.id == "t1" }.categoryId)
        assertTrue(db.merchantRules.isEmpty())
        assertTrue("r" in db.tombstones)
    }

    @Test
    fun noDatabaseIsLoading() = runTest {
        repository.state.value = DatabaseState.NotSelected

        assertTrue(viewModel().state.value.loading)
    }
}
