package com.neteinstein.networth.feature.manage

import com.neteinstein.networth.core.domain.model.AccountType
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
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAlertSettingsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SetAskAccountOnImportUseCase
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
        deleteRule = DeleteMerchantRuleUseCase(edit),
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
        assertEquals(10, state.categoryCount)
        assertEquals(39, state.subcategoryCount)
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
    fun loadingWithoutDatabase() = runTest {
        repository.state.value = DatabaseState.Loading
        val vm = viewModel()

        assertTrue(vm.state.value.loading)
        vm.onEditAccount("nothing")
        assertNull(vm.state.value.editor)
    }
}
