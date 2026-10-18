package com.neteinstein.networth.feature.manage

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.Assumptions
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.RuleMatch
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.AddMemberUseCase
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.FixedClock
import com.neteinstein.networth.core.testing.SequentialIds
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteMerchantRuleUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.ImportMerchantRulesUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAlertSettingsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAssumptionsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveMerchantRuleUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SetAskAccountOnImportUseCase
import com.neteinstein.networth.feature.manage.ui.components.halfSteps
import com.neteinstein.networth.feature.manage.ui.components.percent
import com.neteinstein.networth.feature.manage.ui.screens.ManageViewModel
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ManageTest {
    private fun family(): FamilyDatabase {
        val base = designFamily()
        val pedro = base.accounts.first()
        val groceries = base.categories.first { it.name == "Groceries" }
        return base.copy(
            transactions = listOf(Transaction("t1", pedro.id, LocalDate(2026, 9, 1), -10_00, "x")),
            merchantRules = listOf(
                MerchantRule("r1", "CONTINENTE", RuleMatch.PREFIX, groceries.id, hits = 3),
                MerchantRule("r2", "LIDL", RuleMatch.PREFIX, groceries.id, hits = 9),
                MerchantRule("orphan", "X", RuleMatch.PREFIX, "gone"),
            ),
        )
    }

    private val repository = FakeDatabaseRepository.ready(family())
    private val edit = EditDatabaseUseCase(repository)

    private fun TestScope.viewModel() = ManageViewModel(
        observeDatabase = ObserveDatabaseUseCase(repository),
        saveAccount = SaveAccountUseCase(edit, SequentialIds("new"), FixedClock(LocalDate(2026, 10, 1))),
        deleteAccount = DeleteAccountUseCase(edit),
        setAskAccount = SetAskAccountOnImportUseCase(edit),
        saveAlertSettings = SaveAlertSettingsUseCase(edit),
        saveAssumptions = SaveAssumptionsUseCase(edit),
        deleteRule = DeleteMerchantRuleUseCase(edit),
        saveRule = SaveMerchantRuleUseCase(edit),
        importRules = ImportMerchantRulesUseCase(edit, SequentialIds("rule")),
        addMember = AddMemberUseCase(edit, SequentialIds("person")),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun summarisesTheDatabase() = runTest {
        val state = viewModel().state.value

        assertFalse(state.loading)
        assertEquals(6, state.accounts.size)
        assertTrue(state.accounts.first().hasTransactions)
        assertTrue(state.accounts.last().isChild)
        assertEquals(11, state.categoryCount)
        assertEquals(41, state.subcategoryCount)
        assertEquals(4, state.categoryColors.size)
        assertEquals(listOf("LIDL", "CONTINENTE"), state.rules.map { it.pattern })
        assertEquals("Groceries", state.rules.first().categoryName)
    }

    @Test
    fun addsAnAccountWithAnOpeningBalance() = runTest {
        val vm = viewModel()

        vm.onAddAccount()
        vm.onEditorChange { it.copy(name = " Mortgage ", type = AccountType.MORTGAGE, iban = "pt50 0007 0000", balance = "147.268,16") }
        assertTrue(vm.state.value.editor!!.canSave)
        vm.onSaveAccount()

        assertNull(vm.state.value.editor)
        val mortgage = repository.database.accounts.last()
        assertEquals("Mortgage", mortgage.name)
        assertEquals("PT5000070000", mortgage.iban)
        val snapshot = repository.database.balanceSnapshots.single()
        assertEquals(147_268_16, snapshot.amountMinor)
        assertEquals(LocalDate(2026, 10, 1), snapshot.date)
        assertEquals(147_268_16, vm.state.value.accounts.last().balanceMinor)
    }

    @Test
    fun addingAPersonFromTheEditorMakesThemAnOwner() = runTest {
        val vm = viewModel()

        vm.onAddAccount()
        vm.onAddPerson("Avó Rosa", MemberRole.ADULT)

        val person = repository.database.members.last()
        assertEquals("Avó Rosa", person.name)
        assertEquals(listOf(person.id), vm.state.value.editor!!.ownerIds)
    }

    @Test
    fun invalidBalancesCannotBeSaved() = runTest {
        val vm = viewModel()
        vm.onAddAccount()
        vm.onEditorChange { it.copy(name = "X", balance = "abc") }

        vm.onSaveAccount()

        assertFalse(vm.state.value.editor!!.balanceIsValid)
        assertEquals(6, repository.database.accounts.size)
        vm.onDismissEditor()
        assertNull(vm.state.value.editor)
    }

    @Test
    fun editingUpdatesTodaysSnapshotAndDeleteNeedsNoTransactions() = runTest {
        val vm = viewModel()
        val joint = repository.database.accounts.first { it.name == "Joint savings" }
        val pedro = repository.database.accounts.first()

        vm.onEditAccount(joint.id)
        vm.onEditorChange { it.copy(balance = "100") }
        vm.onSaveAccount()
        vm.onEditAccount(joint.id)
        vm.onEditorChange { it.copy(balance = "200", bank = "BPI") }
        vm.onSaveAccount()
        assertEquals(listOf(200_00L), repository.database.balanceSnapshots.map { it.amountMinor })
        assertEquals("BPI", repository.database.account(joint.id)?.bank)

        vm.onEditAccount(pedro.id)
        assertFalse(vm.state.value.editor!!.canDelete)
        vm.onDeleteAccount()
        assertNotNull(repository.database.account(pedro.id))

        vm.onEditAccount(joint.id)
        vm.onDeleteAccount()
        assertNull(repository.database.account(joint.id))
        assertTrue(repository.database.balanceSnapshots.isEmpty())
        assertTrue(joint.id in repository.database.tombstones)
    }

    @Test
    fun settingsAndRules() = runTest {
        val vm = viewModel()

        vm.onAskAccountChange(false)
        vm.onSaveAlerts(spikeThresholdPct = 5, largePaymentMinor = 500_00, newSubscriptions = false)
        vm.onDeleteRule("r1")

        val settings = repository.database.settings
        assertFalse(settings.askAccountOnImport)
        assertEquals(10, settings.alerts.spikeThresholdPct)
        assertEquals(500_00, settings.alerts.largePaymentMinor)
        assertFalse(settings.alerts.newSubscriptions)
        assertEquals(listOf("r2", "orphan"), repository.database.merchantRules.map { it.id })
        assertTrue("r1" in repository.database.tombstones)
    }

    @Test
    fun editsARuleForTheFutureOrAlsoThePast() = runTest {
        val db = repository.database
        val pedro = db.accounts.first()
        val groceries = db.categories.first { it.name == "Groceries" }
        val dining = db.categories.first { it.name == "Dining & leisure" }
        val restaurants = dining.subcategories.first { it.name == "Restaurants" }
        repository.state.value = (repository.state.value as DatabaseState.Ready).copy(
            database = db.copy(
                transactions = listOf(
                    Transaction("c1", pedro.id, LocalDate(2026, 9, 1), -10_00, "COMPRA CONTINENTE MATOSINHOS", categoryId = groceries.id),
                    Transaction("c2", pedro.id, LocalDate(2026, 9, 2), -5_00, "COMPRA CONTINENTE BOM DIA"),
                    Transaction("c3", pedro.id, LocalDate(2026, 9, 3), 5_00, "TRF DE CONTINENTE"),
                    Transaction("c4", pedro.id, LocalDate(2026, 9, 4), -5_00, "COMPRA CONTINENTE", isInternalTransfer = true),
                    Transaction("l1", pedro.id, LocalDate(2026, 9, 5), -7_00, "COMPRA LIDL PORTO"),
                ),
            ),
        )
        val vm = viewModel()
        vm.onEditRule("r1")
        val editor = assertNotNull(vm.state.value.ruleEditor)
        assertEquals(2, editor.pastMatches)
        assertEquals(1, editor.pastToRefile)

        vm.onRuleCategory(dining.id)
        vm.onRuleChange { it.copy(subcategoryId = restaurants.id) }
        assertEquals(2, vm.state.value.ruleEditor!!.pastToRefile)
        vm.onRuleCategory(groceries.id)
        assertNull(vm.state.value.ruleEditor!!.rule.subcategoryId)
        vm.onRuleCategory(dining.id)
        vm.onRuleMatch(RuleMatch.CONTAINS)
        vm.onRuleChange { it.copy(pattern = "") }
        assertFalse(vm.state.value.ruleEditor!!.canSave)
        vm.onSaveRule(applyToPast = false)
        assertNotNull(vm.state.value.ruleEditor)
        vm.onRuleChange { it.copy(pattern = " CONTINENTE ") }

        // Only from now on: the rule changes, the past stays as it was.
        vm.onSaveRule(applyToPast = false)
        assertNull(vm.state.value.ruleEditor)
        val rule = repository.database.merchantRules.first { it.id == "r1" }
        assertEquals("CONTINENTE", rule.pattern)
        assertEquals(RuleMatch.CONTAINS, rule.match)
        assertEquals(dining.id, rule.categoryId)
        assertEquals(groceries.id, repository.database.transactions.first { it.id == "c1" }.categoryId)

        // Also the past: every matching outflow is re-filed; incoming money and internal transfers are left alone.
        vm.onEditRule("r1")
        vm.onRuleChange { it.copy(subcategoryId = restaurants.id) }
        vm.onSaveRule(applyToPast = true)
        val txs = repository.database.transactions.associateBy { it.id }
        assertEquals(dining.id, txs.getValue("c1").categoryId)
        assertEquals(restaurants.id, txs.getValue("c2").subcategoryId)
        assertNull(txs.getValue("c3").categoryId)
        assertNull(txs.getValue("c4").categoryId)
        assertNull(txs.getValue("l1").categoryId)
        assertEquals(2, SaveMerchantRuleUseCase(edit)(rule.copy(categoryId = groceries.id), applyToPast = true))

        vm.onEditRule("r2")
        vm.onDismissRule()
        assertNull(vm.state.value.ruleEditor)
        vm.onEditRule("r2")
        vm.onDeleteRule("r2")
        assertNull(vm.state.value.ruleEditor)
        vm.onEditRule("missing")
        assertNull(vm.state.value.ruleEditor)
    }

    @Test
    fun assumptionsAreSavedWithinTheDesignRanges() = runTest {
        val vm = viewModel()
        assertEquals(Assumptions(), vm.state.value.assumptions)

        vm.onSaveAssumptions(Assumptions(returnRatePct = 4.5, incomeGrowthPct = 1.0, priceRisePct = 3.0, emergencyFundMonths = 9))
        assertEquals(Assumptions(4.5, 1.0, 3.0, 9), repository.database.settings.assumptions)
        assertEquals(Assumptions(4.5, 1.0, 3.0, 9), vm.state.value.assumptions)

        vm.onSaveAssumptions(Assumptions(returnRatePct = 20.0, incomeGrowthPct = -1.0, priceRisePct = 9.0, emergencyFundMonths = 0))
        assertEquals(Assumptions(7.0, 0.0, 6.0, 1), repository.database.settings.assumptions)
    }

    @Test
    fun assumptionFormatting() {
        assertEquals(2.5, halfSteps(2.4f))
        assertEquals(3.0, halfSteps(2.9f))
        assertEquals("3", percent(3.0))
        assertEquals("2.5", percent(2.5))
        assertEquals("2.5", percent(2.46))
    }

    @Test
    fun loadingWithoutDatabase() = runTest {
        repository.state.value = DatabaseState.Loading
        val vm = viewModel()

        assertTrue(vm.state.value.loading)
        vm.onEditAccount("nothing")
        assertNull(vm.state.value.editor)
    }
}
