package com.neteinstein.networth.feature.importer

import com.neteinstein.networth.core.designsystem.ui.files.PickedFile
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.AddMemberUseCase
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.testing.FakeAuthRepository
import com.neteinstein.networth.core.testing.FakeCategorizer
import com.neteinstein.networth.core.testing.FakeClaudeKeyRepository
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.FixedClock
import com.neteinstein.networth.core.testing.SequentialIds
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.importer.domain.model.ImportRequest
import com.neteinstein.networth.feature.importer.domain.parse.ParseFailure
import com.neteinstein.networth.feature.importer.domain.parse.StatementParser
import com.neteinstein.networth.feature.importer.domain.usecase.CategoriseWithClaudeUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.CommitImportUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.ParseStatementUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.SaveReviewUseCase
import com.neteinstein.networth.feature.importer.ui.Periods
import com.neteinstein.networth.feature.importer.ui.screens.FileError
import com.neteinstein.networth.feature.importer.ui.screens.ImportStage
import com.neteinstein.networth.feature.importer.ui.screens.ImportViewModel
import com.neteinstein.networth.feature.importer.ui.screens.ReviewViewModel
import com.neteinstein.networth.feature.importer.ui.screens.isMatched
import com.neteinstein.networth.feature.importer.ui.screens.previewImportState
import com.neteinstein.networth.feature.importer.ui.screens.previewReviewState
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelsTest {
    private val family = designFamily()
    private val pedro = family.members.first { it.name == "Pedro" }
    private val maria = family.members.first { it.name == "Maria" }
    private val repository = FakeDatabaseRepository.ready(family)
    private val edit = EditDatabaseUseCase(repository)
    private val clock = FixedClock(LocalDate(2026, 10, 1))
    private val noClaude =
        CategoriseWithClaudeUseCase(FakeClaudeKeyRepository(), FakeCategorizer(), ObserveDatabaseUseCase(repository), edit)

    private val csv =
        """
        IBAN: PT50 1111 2222 3333 4444 5555 6
        Data;Descrição;Montante;Saldo
        02-09-2026;Compra Mb Cartão ****1* Pingo Doce;-10,00;90,00
        03-09-2026;Levantamento Mb;-20,00;70,00
        """.trimIndent().encodeToByteArray()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.importViewModel(): ImportViewModel {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return ImportViewModel(
            observeDatabase = ObserveDatabaseUseCase(repository),
            parse = ParseStatementUseCase(StatementParser(), dispatcher),
            commit = CommitImportUseCase(
                edit,
                SequentialIds("imp"),
                clock,
                ObserveAuthUseCase(FakeAuthRepository(signedIn = true)),
                noClaude,
            ),
            addMember = AddMemberUseCase(edit, SequentialIds("person")),
            clock = clock,
        ).also { vm -> backgroundScope.launch(dispatcher) { vm.state.collect {} } }
    }

    private fun TestScope.reviewViewModel() =
        ReviewViewModel(ObserveDatabaseUseCase(repository), SaveReviewUseCase(edit, SequentialIds("rule")))
            .also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @Test
    fun picksOwnersAddsAPersonAndImportsIntoANewAccount() =
        runTest {
            val vm = importViewModel()
            assertTrue(vm.state.value.ready)
            assertEquals(ImportStage.PICK, vm.state.value.stage)

            vm.onFilesPicked(listOf(PickedFile("extrato.csv", csv)))
            val confirm = vm.state.value
            assertEquals(ImportStage.CONFIRM, confirm.stage)
            val section = confirm.sections.single()
            assertNull(section.choice.accountId)
            assertFalse(section.isMatched)
            assertEquals(2, section.rows)

            vm.onNewAccountName(0, "Conta conjunta")
            vm.onNewAccountType(0, AccountType.SAVINGS)
            vm.onOwnerToggled(0, pedro.id)
            vm.onOwnerToggled(0, maria.id)
            vm.onOwnerToggled(0, maria.id)
            vm.onAddPerson(0, "Avó Rosa", MemberRole.ADULT)
            val person = repository.database.members.first { it.name == "Avó Rosa" }
            assertEquals(listOf(pedro.id, person.id), vm.state.value.sections.single().choice.ownerIds)
            assertTrue(vm.state.value.canImport)

            vm.onImport()
            val done = vm.state.value
            assertEquals(ImportStage.DONE, done.stage)
            val summary = done.results.single().summary
            assertEquals(2, summary.imported)
            assertEquals(listOf("Conta conjunta"), summary.accountNames)
            val account = repository.database.accounts.first { it.name == "Conta conjunta" }
            assertEquals(AccountType.SAVINGS, account.type)
            assertEquals(listOf(pedro.id, person.id), account.ownerIds)
            assertEquals("PT50111122223333444455556", account.iban)
            assertEquals("Pedro Vicente", repository.database.imports.single().importedBy)
            assertEquals(2, done.pendingReview)
            assertEquals(1, done.history.size)
            assertTrue(done.coverage.any { it.account.id == account.id })

            vm.onStartOver()
            assertEquals(ImportStage.PICK, vm.state.value.stage)

            // The same file again: matched by IBAN, nothing new.
            vm.onFilesPicked(listOf(PickedFile("extrato.csv", csv)))
            val again = vm.state.value.sections.single()
            assertEquals(account.id, again.choice.accountId)
            assertTrue(again.isMatched)
            vm.onImport()
            val result = vm.state.value.results.single()
            assertEquals(2, result.summary.duplicatesSkipped)
            assertEquals(listOf(LocalDate(2026, 9, 2) to LocalDate(2026, 9, 3)), result.overlaps)
        }

    @Test
    fun selectingAnAccountTakesItsOwnersAndSkippingEverythingDisablesImport() =
        runTest {
            val vm = importViewModel()
            vm.onFilesPicked(listOf(PickedFile("extrato.csv", csv)))
            val joint = family.accounts.first { it.type == AccountType.SAVINGS && it.ownerIds.size == 2 }
            vm.onAccountSelected(0, joint.id)
            assertEquals(joint.ownerIds, vm.state.value.sections.single().choice.ownerIds)
            vm.onIncludeSection(0, false)
            assertFalse(vm.state.value.canImport)
            vm.onImport()
            assertEquals(ImportStage.CONFIRM, vm.state.value.stage)
        }

    @Test
    fun importsStraightAwayWhenTheAccountIsKnownAndAskingIsOff() =
        runTest {
            val pedroAccount = family.accounts.first { it.name == "Pedro" }
            repository.state.value =
                (repository.state.value as DatabaseState.Ready).let { ready ->
                    ready.copy(
                        database =
                            ready.database.copy(
                                settings = ready.database.settings.copy(askAccountOnImport = false),
                                accounts = ready.database.accounts.map {
                                    if (it.id ==
                                        pedroAccount.id
                                    ) {
                                        it.copy(iban = "PT50111122223333444455556")
                                    } else {
                                        it
                                    }
                                },
                            ),
                    )
                }
            val vm = importViewModel()
            vm.onFilesPicked(listOf(PickedFile("extrato.csv", csv)))
            assertEquals(ImportStage.DONE, vm.state.value.stage)
            assertEquals(2, repository.database.transactions.count { it.accountId == pedroAccount.id })
        }

    @Test
    fun importsSeveralFilesOneAfterTheOther() =
        runTest {
            val october =
                """
                IBAN: PT50 1111 2222 3333 4444 5555 6
                Data;Descrição;Montante;Saldo
                02-10-2026;Compra Mb Cartão ****1* Continente;-5,00;65,00
                """.trimIndent().encodeToByteArray()
            val vm = importViewModel()
            vm.onFilesPicked(
                listOf(
                    PickedFile("setembro.csv", csv),
                    PickedFile("notes.csv", "nothing to see".encodeToByteArray()),
                    PickedFile("outubro.csv", october),
                    PickedFile("novembro.csv", october),
                ),
            )
            val first = vm.state.value
            assertEquals(ImportStage.CONFIRM, first.stage)
            assertEquals(1, first.fileNumber)
            assertEquals(4, first.fileCount)
            assertEquals("setembro.csv", first.fileName)
            vm.onReuseChoices(false)
            vm.onNewAccountName(0, "Conta conjunta")
            vm.onImport()

            // The unreadable file is reported and skipped; the next one already knows the new account.
            val second = vm.state.value
            assertEquals(ImportStage.CONFIRM, second.stage)
            assertEquals(3, second.fileNumber)
            assertEquals("notes.csv", second.errors.single().fileName)
            assertTrue(second.sections.single().isMatched)
            vm.onImport()

            vm.onSkipFile()
            val done = vm.state.value
            assertEquals(ImportStage.DONE, done.stage)
            assertEquals(listOf("setembro.csv", "outubro.csv"), done.results.map { it.summary.fileName })
            assertEquals(3, repository.database.transactions.size - family.transactions.size)
            assertEquals(2, repository.database.imports.size - family.imports.size)

            vm.onSkipFile()
            assertEquals(ImportStage.DONE, vm.state.value.stage)
            vm.onFilesPicked(emptyList())
            assertEquals(ImportStage.DONE, vm.state.value.stage)
        }

    @Test
    fun reusesTheAccountAndOwnersForTheOtherFilesOfTheSameAccount() =
        runTest {
            fun month(m: Int) =
                """
                Data;Descrição;Montante;Saldo
                02-0$m-2026;Compra Mb Cartão ****1* Pingo Doce;-10,00;90,00
                """.trimIndent().encodeToByteArray()
            val vm = importViewModel()
            vm.onFilesPicked((1..3).map { PickedFile("extrato_0$it.csv", month(it)) })
            assertTrue(vm.state.value.hasMoreFiles)
            assertTrue(vm.state.value.reuseChoices)
            vm.onNewAccountName(0, "Conta da casa")
            vm.onOwnerToggled(0, pedro.id)
            vm.onOwnerToggled(0, maria.id)
            vm.onImport()

            // Months 2 and 3 have no IBAN either, but are the same account: imported without asking.
            val done = vm.state.value
            assertEquals(ImportStage.DONE, done.stage)
            assertEquals(3, done.results.size)
            val account = repository.database.accounts.single { it.name == "Conta da casa" }
            assertEquals(listOf(pedro.id, maria.id), account.ownerIds)
            assertEquals(3, repository.database.transactions.count { it.accountId == account.id })
        }

    @Test
    fun asksAgainForEachFileWhenReuseIsOff() =
        runTest {
            val vm = importViewModel()
            vm.onFilesPicked(listOf(PickedFile("a.csv", csv), PickedFile("b.csv", csv)))
            vm.onReuseChoices(false)
            vm.onIncludeSection(0, false)
            vm.onSkipFile()
            assertEquals(ImportStage.CONFIRM, vm.state.value.stage)
            assertFalse(vm.state.value.hasMoreFiles)
            assertFalse(vm.state.value.sections.single().choice.include.not())
        }

    @Test
    fun skipsFilesWhoseAccountsWereAllLeftOut() =
        runTest {
            val other =
                """
                IBAN: PT50 1111 2222 3333 4444 5555 6
                Data;Descrição;Montante;Saldo
                05-10-2026;Compra Mb Cartão ****1* Lidl;-1,00;69,00
                """.trimIndent().encodeToByteArray()
            val vm = importViewModel()
            vm.onFilesPicked(listOf(PickedFile("a.csv", csv), PickedFile("b.csv", other)))
            vm.onAccountSelected(0, family.accounts.first().id)
            vm.onIncludeSection(0, false)
            vm.onIncludeSection(0, true)
            vm.onImport()
            assertEquals(ImportStage.DONE, vm.state.value.stage)
            assertEquals(2, vm.state.value.results.size)
        }

    @Test
    fun skippingEveryFileGoesBackToPicking() =
        runTest {
            val vm = importViewModel()
            vm.onFilesPicked(listOf(PickedFile("a.csv", csv), PickedFile("b.csv", csv)))
            vm.onSkipFile()
            vm.onSkipFile()
            assertEquals(ImportStage.PICK, vm.state.value.stage)
            assertTrue(vm.state.value.results.isEmpty())
        }

    @Test
    fun showsReadableErrors() =
        runTest {
            val vm = importViewModel()
            vm.onFilesPicked(listOf(PickedFile("notes.csv", "nothing to see".encodeToByteArray())))
            assertEquals(ParseFailure.NO_HEADER_ROW, vm.state.value.errors.single().failure)
            assertEquals(ImportStage.PICK, vm.state.value.stage)
            vm.onDismissError()
            assertTrue(vm.state.value.errors.isEmpty())
            vm.onPickFailed()
            assertEquals(listOf(FileError(null, null)), vm.state.value.errors)
            repository.state.value = DatabaseState.NotSelected
            assertFalse(vm.state.value.ready)
        }

    @Test
    fun commitNeedsAnOpenDatabase() =
        runTest {
            repository.state.value = DatabaseState.NotSelected
            val commit = CommitImportUseCase(edit, SequentialIds(), clock, ObserveAuthUseCase(FakeAuthRepository()), noClaude)
            val statement = StatementParser().parse("a.csv", csv)
            assertFailsWith<IllegalStateException> { commit(ImportRequest(statement, emptyList())) }
            assertTrue(FakeAuthRepository().state.value is AuthState.SignedOut)
        }

    @Test
    fun reviewPicksAcceptsSuggestionsAndLearns() =
        runTest {
            val groceries = family.categories.first { it.name == "Groceries" }
            val other = family.categories.first { it.name == "Other" }
            val account = family.accounts.first().id
            repository.state.value =
                (repository.state.value as DatabaseState.Ready).let { ready ->
                    ready.copy(
                        database =
                            ready.database.copy(
                                transactions =
                                    listOf(
                                        Transaction(
                                            "p1",
                                            account,
                                            LocalDate(2026, 8, 1),
                                            -500,
                                            "Compra Mb Cartão ****1* Continente Porto",
                                            categoryId = groceries.id,
                                        ),
                                        Transaction("w1", account, LocalDate(2026, 9, 3), -700, "Compra Mb Cartão ****1* Continente Maia"),
                                        Transaction("w2", account, LocalDate(2026, 9, 2), -900, "Compra Mb Cartão ****1* Continente Braga"),
                                        Transaction("w3", account, LocalDate(2026, 9, 1), -300, "MB WAY P/ 9** *** 412"),
                                    ),
                            ),
                    )
                }
            val vm = reviewViewModel()
            val initial = vm.state.value
            assertEquals(3, initial.total)
            assertEquals(0, initial.done)
            assertEquals(groceries.id, initial.items.first().shownCategoryId)
            assertTrue(initial.items.first().remember)

            vm.onAcceptAll()
            assertEquals(2, vm.state.value.done)
            vm.onPick("w3", groceries.id)
            // Tapping the picked category again clears a wrong pick.
            vm.onPick("w3", groceries.id)
            assertEquals(2, vm.state.value.done)
            vm.onPick("w3", other.id)
            vm.onNote("w3", "Football club")
            vm.onRemember("w3", false)
            vm.onRemember("w1", false)
            vm.onRemember("w2", false)
            assertEquals(3, vm.state.value.done)

            vm.onSave()
            assertEquals(3, vm.state.value.lastSaved)
            assertEquals(0, vm.state.value.total)
            val txs = repository.database.transactions.associateBy { it.id }
            assertEquals(other.id, txs.getValue("w3").categoryId)
            assertEquals("Football club", txs.getValue("w3").note)
            assertTrue(repository.database.merchantRules.isEmpty())
            // Nothing picked → nothing to save.
            vm.onSave()
            assertEquals(3, vm.state.value.lastSaved)
        }

    @Test
    fun describesStatementPeriods() {
        val short = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val long =
            listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

        fun label(a: LocalDate, b: LocalDate) = Periods.label(a, b, short, long)
        assertEquals("September 2026", label(LocalDate(2026, 9, 1), LocalDate(2026, 10, 1)))
        assertEquals("December 2025", label(LocalDate(2025, 12, 1), LocalDate(2025, 12, 31)))
        assertEquals("22–28 Sep 2026", label(LocalDate(2026, 9, 22), LocalDate(2026, 9, 28)))
        assertEquals("28 Sep – 4 Oct 2026", label(LocalDate(2026, 9, 28), LocalDate(2026, 10, 4)))
        assertEquals("28 Dec 2025 – 4 Jan 2026", label(LocalDate(2025, 12, 28), LocalDate(2026, 1, 4)))
        assertEquals("3 Sep 2026", label(LocalDate(2026, 9, 3), LocalDate(2026, 9, 3)))
        assertEquals(Periods.Kind.WEEKLY, Periods.kind(LocalDate(2026, 9, 22), LocalDate(2026, 9, 28)))
        assertEquals(Periods.Kind.MONTHLY, Periods.kind(LocalDate(2026, 8, 1), LocalDate(2026, 8, 31)))
        assertEquals(Periods.Kind.OTHER, Periods.kind(LocalDate(2026, 8, 3), LocalDate(2026, 8, 31)))
    }

    @Test
    fun previewDataIsConsistent() {
        assertEquals(ImportStage.DONE, previewImportState(ImportStage.DONE).stage)
        assertTrue(previewImportState(ImportStage.CONFIRM).canImport)
        assertEquals(3, previewReviewState().items.size)
    }
}
