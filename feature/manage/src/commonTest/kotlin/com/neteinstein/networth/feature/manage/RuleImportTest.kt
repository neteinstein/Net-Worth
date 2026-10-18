package com.neteinstein.networth.feature.manage

import com.neteinstein.networth.core.designsystem.ui.files.PickedFile
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.RuleMatch
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.usecase.AddMemberUseCase
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.FixedClock
import com.neteinstein.networth.core.testing.SequentialIds
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.manage.domain.RuleFile
import com.neteinstein.networth.feature.manage.domain.RuleFileEntry
import com.neteinstein.networth.feature.manage.domain.RuleFileError
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteMerchantRuleUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.ImportMerchantRulesUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAlertSettingsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAssumptionsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveMerchantRuleUseCase
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RuleImportTest {
    private val base = designFamily()
    private val groceries = base.categories.first { it.name == "Groceries" }
    private val supermarket = groceries.subcategories.first { it.name == "Supermarket" }
    private val kids = base.categories.first { it.name == "Kids’ activities" }
    private val transport = base.categories.first { it.name == "Transport" }
    private val fuel = transport.subcategories.first { it.name == "Fuel" }

    private val file = """
        {
          "rules": [
            {"pattern": "lidl", "match": "PREFIX", "category": "Groceries", "subcategory": "Supermarket"},
            {"pattern": "POSTO CEPSA", "category": "transport", "subcategory": "fuel"},
            {"pattern": "DECATHLON", "match": "prefix", "category": "Kids' activities", "subcategory": "Sports"},
            {"pattern": "BOLT.EUO", "match": "CONTAINS", "category": "Transport", "subcategory": "Public transport"},
            {"pattern": "COLEGIO", "category": "School", "subcategory": "Tuition"},
            {"pattern": "FOO", "category": "Pets"},
            {"pattern": "", "category": "Groceries"},
            {"pattern": "NO CATEGORY"},
            {"pattern": "BAD", "match": "REGEX", "category": "Groceries"},
            "not a rule"
          ]
        }
    """.trimIndent()

    private fun family(): FamilyDatabase {
        val account = base.accounts.first().id
        return base.copy(
            transactions = listOf(
                Transaction("t1", account, LocalDate(2026, 9, 1), -10_00, "Compra Mb Cartão ****123* Lidl Benfica Lisboa"),
                Transaction(
                    "t2",
                    account,
                    LocalDate(2026, 9, 2),
                    -40_00,
                    "Compra Mb Cartão ****123* Posto Cepsa Almada",
                    categoryId = groceries.id,
                ),
                Transaction(
                    "t3",
                    account,
                    LocalDate(2026, 9, 3),
                    -5_00,
                    "Compra Mb Cartão ****123* Lidl Agradece",
                    categoryId = groceries.id,
                    subcategoryId = supermarket.id,
                ),
                Transaction("t4", account, LocalDate(2026, 9, 4), -5_00, "Compra Mb Cartão ****123* Lidl Faro", isInternalTransfer = true),
                Transaction("t5", account, LocalDate(2026, 9, 5), 5_00, "Compra Mb Cartão ****123* Lidl Porto"),
            ),
            merchantRules = listOf(
                MerchantRule("d", "DECATHLON", RuleMatch.PREFIX, kids.id, kids.subcategories.first { it.name == "Sports" }.id, hits = 4),
                MerchantRule("c", "POSTO CEPSA", RuleMatch.PREFIX, groceries.id, hits = 2),
            ),
        )
    }

    private val repository = FakeDatabaseRepository.ready(family())
    private val edit = EditDatabaseUseCase(repository)
    private val importRules = ImportMerchantRulesUseCase(edit, SequentialIds("rule"))

    private fun TestScope.viewModel() = ManageViewModel(
        observeDatabase = ObserveDatabaseUseCase(repository),
        saveAccount = SaveAccountUseCase(edit, SequentialIds("new"), FixedClock(LocalDate(2026, 10, 1))),
        deleteAccount = DeleteAccountUseCase(edit),
        setAskAccount = SetAskAccountOnImportUseCase(edit),
        saveAlertSettings = SaveAlertSettingsUseCase(edit),
        saveAssumptions = SaveAssumptionsUseCase(edit),
        deleteRule = DeleteMerchantRuleUseCase(edit),
        saveRule = SaveMerchantRuleUseCase(edit),
        importRules = importRules,
        addMember = AddMemberUseCase(edit, SequentialIds("person")),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun readsRulesAndLeavesOutBrokenLines() {
        val content = RuleFile.read(file)

        assertNull(content.error)
        assertEquals(6, content.entries.size)
        assertEquals(4, content.invalid)
        assertEquals(RuleFileEntry("LIDL", RuleMatch.PREFIX, "Groceries", "Supermarket"), content.entries.first())
        assertEquals(RuleMatch.PREFIX, content.entries.first { it.pattern == "POSTO CEPSA" }.match)
        assertEquals(RuleMatch.CONTAINS, content.entries.first { it.pattern == "BOLT.EUO" }.match)
    }

    @Test
    fun readsABareListAndKeepsTheLastOfARepeatedRule() {
        val content = RuleFile.read(
            "[{\"pattern\": \"LIDL\", \"category\": \"Groceries\"}, {\"pattern\": \"lidl\", \"category\": \"Transport\", \"subcategory\": null}]",
        )

        assertEquals(listOf(RuleFileEntry("LIDL", RuleMatch.PREFIX, "Transport")), content.entries)
    }

    @Test
    fun reportsFilesThatAreNotRules() {
        assertEquals(RuleFileError.NOT_JSON, RuleFile.read("pattern;category").error)
        assertEquals(RuleFileError.NO_RULES, RuleFile.read("{\"other\": []}").error)
        assertEquals(RuleFileError.NOT_JSON, RuleFile.read("\"text\"").error)
        assertEquals(RuleFileError.NO_RULES, RuleFile.read("{\"rules\": [{\"pattern\": \"X\"}]}").error)
    }

    @Test
    fun plansAgainstTheHouseholdsCategoriesAndRules() {
        val db = family()
        val plan = RuleFile.plan(RuleFile.read(file).entries, db.categories, db.merchantRules)

        assertEquals(listOf("LIDL", "BOLT.EUO"), plan.toAdd.map { it.pattern })
        assertEquals(supermarket.id, plan.toAdd.first().subcategoryId)
        assertEquals(listOf("c"), plan.toUpdate.map { it.id })
        assertEquals(fuel.id, plan.toUpdate.single().subcategoryId)
        assertEquals(transport.id, plan.toUpdate.single().categoryId)
        assertEquals(1, plan.unchanged)
        assertEquals(listOf("School › Tuition", "Pets"), plan.unknownCategories)
        assertEquals(4, plan.rules.size)
        assertTrue(plan.hasChanges)
        assertFalse(RuleFile.plan(emptyList(), db.categories, db.merchantRules).hasChanges)
    }

    @Test
    fun importsForTheFutureOnly() = runTest {
        val refiled = importRules(RuleFile.read(file).entries, applyToPast = false)

        val rules = repository.database.merchantRules
        assertEquals(0, refiled)
        assertEquals(listOf("d", "c", "rule-1", "rule-2"), rules.map { it.id })
        assertEquals(transport.id, rules.first { it.id == "c" }.categoryId)
        assertEquals(2, rules.first { it.id == "c" }.hits)
        assertEquals(null, repository.database.transactions.first { it.id == "t1" }.categoryId)
    }

    @Test
    fun importsAndRefilesThePast() = runTest {
        val refiled = importRules(RuleFile.read(file).entries, applyToPast = true)

        val byId = repository.database.transactions.associateBy { it.id }
        assertEquals(2, refiled)
        assertEquals(supermarket.id, byId.getValue("t1").subcategoryId)
        assertEquals(fuel.id, byId.getValue("t2").subcategoryId)
        // Already filed this way, a transfer between the family's accounts, and money in stay as they were.
        assertEquals(0, byId.getValue("t3").updatedAt)
        assertNull(byId.getValue("t4").categoryId)
        assertNull(byId.getValue("t5").categoryId)
    }

    @Test
    fun viewModelPreviewsThenImports() = runTest {
        val vm = viewModel()

        vm.onRuleFilePicked(emptyList())
        assertNull(vm.state.value.ruleImport)

        vm.onRuleFilePicked(listOf(PickedFile("rules.json", file.encodeToByteArray())))
        val preview = vm.state.value.ruleImport!!
        assertEquals("rules.json", preview.fileName)
        assertEquals(2, preview.toAdd)
        assertEquals(1, preview.toUpdate)
        assertEquals(1, preview.unchanged)
        assertEquals(4, preview.invalid)
        assertEquals(2, preview.pastToRefile)
        assertTrue(preview.canImport)

        vm.onImportRules(applyToPast = true)
        assertNull(vm.state.value.ruleImport)
        assertEquals(4, repository.database.merchantRules.size)
        assertEquals(supermarket.id, repository.database.transactions.first { it.id == "t1" }.subcategoryId)

        vm.onRuleFilePicked(listOf(PickedFile("rules.json", file.encodeToByteArray())))
        assertFalse(vm.state.value.ruleImport!!.canImport)
        vm.onDismissRuleImport()
        assertNull(vm.state.value.ruleImport)
    }

    @Test
    fun viewModelExplainsUnreadableFiles() = runTest {
        val vm = viewModel()

        vm.onRuleFilePickFailed()
        assertEquals(RuleFileError.NOT_JSON, vm.state.value.ruleImport!!.error)
        assertFalse(vm.state.value.ruleImport!!.canImport)
        vm.onImportRules(applyToPast = false)
        assertEquals(2, repository.database.merchantRules.size)

        vm.onRuleFilePicked(listOf(PickedFile("notes.json", "{\"rules\": []}".encodeToByteArray())))
        assertEquals(RuleFileError.NO_RULES, vm.state.value.ruleImport!!.error)
    }
}
