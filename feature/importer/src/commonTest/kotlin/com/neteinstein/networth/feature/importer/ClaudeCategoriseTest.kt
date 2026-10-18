package com.neteinstein.networth.feature.importer

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.repository.CategorizerFailure
import com.neteinstein.networth.core.domain.repository.CategoryGuess
import com.neteinstein.networth.core.domain.repository.DatabaseState
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
import com.neteinstein.networth.feature.importer.domain.model.ClaudeOutcome
import com.neteinstein.networth.feature.importer.domain.model.ImportRequest
import com.neteinstein.networth.feature.importer.domain.model.ImportSummary
import com.neteinstein.networth.feature.importer.domain.model.SectionTarget
import com.neteinstein.networth.feature.importer.domain.parse.StatementParser
import com.neteinstein.networth.feature.importer.domain.usecase.CategoriseWithClaudeUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.CommitImportUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClaudeCategoriseTest {
    private val family = designFamily()
    private val repository = FakeDatabaseRepository.ready(family)
    private val edit = EditDatabaseUseCase(repository)
    private val health = family.categories.first { it.subcategories.isNotEmpty() }
    private val other = family.categories.first { it.name == "Other" }

    private val csv =
        """
        IBAN: PT50 1111 2222 3333 4444 5555 6
        Data;Descrição;Montante;Saldo
        02-09-2026;Compra Mb Cartão ****1* Farmacia Estacio;-10,00;90,00
        03-09-2026;Levantamento Mb;-20,00;70,00
        04-09-2026;Compra Mb Cartão ****1* Loja Desconhecida;-5,00;65,00
        """.trimIndent().encodeToByteArray()

    private val keys = FakeClaudeKeyRepository("sk-ant-test")

    private fun commit(categorizer: FakeCategorizer): CommitImportUseCase {
        val claude = CategoriseWithClaudeUseCase(keys, categorizer, ObserveDatabaseUseCase(repository), edit)
        return CommitImportUseCase(
            edit,
            SequentialIds("imp"),
            FixedClock(LocalDate(2026, 10, 1)),
            ObserveAuthUseCase(FakeAuthRepository()),
            claude,
        )
    }

    private suspend fun CommitImportUseCase.import(): ImportSummary {
        val statement = StatementParser().parse("extrato.csv", csv)
        return this(ImportRequest(statement, listOf(SectionTarget(0, null, "Conta", AccountType.CURRENT, emptyList()))))
    }

    @Test
    fun claudeFilesWhatRulesCouldNot() = runTest {
        val categorizer =
            FakeCategorizer(
                mapOf(
                    "Farmacia" to CategoryGuess(health.id, health.subcategories.first().id),
                    "Levantamento" to CategoryGuess(other.id, "not-a-subcategory"),
                    "Desconhecida" to CategoryGuess("deleted-category", null),
                ),
            )

        val summary = commit(categorizer).import()

        val sent = categorizer.calls.single()
        assertEquals(3, sent.size)
        assertEquals(ClaudeOutcome(filed = 2), summary.claude)
        assertEquals(2, summary.autoCategorised)
        assertEquals(1, summary.needReview)
        val transactions = repository.database.transactions.associateBy { it.description }
        val pharmacy = transactions.getValue("Compra Mb Cartão ****1* Farmacia Estacio")
        assertEquals(health.id, pharmacy.categoryId)
        assertEquals(health.subcategories.first().id, pharmacy.subcategoryId)
        val cash = transactions.getValue("Levantamento Mb")
        assertEquals(other.id, cash.categoryId)
        assertNull(cash.subcategoryId)
        assertTrue(transactions.getValue("Compra Mb Cartão ****1* Loja Desconhecida").needsReview)
    }

    @Test
    fun failuresLeaveTheImportInPlace() = runTest {
        val summary = commit(FakeCategorizer(failure = CategorizerFailure.INVALID_KEY)).import()

        assertEquals(ClaudeOutcome(filed = 0, failure = CategorizerFailure.INVALID_KEY), summary.claude)
        assertEquals(3, summary.imported)
        assertEquals(3, summary.needReview)
        assertEquals(3, repository.database.transactions.count { it.needsReview })
    }

    @Test
    fun withoutAKeyClaudeIsNotAsked() = runTest {
        keys.setApiKey(null)
        val categorizer = FakeCategorizer()

        val summary = commit(categorizer).import()

        assertNull(summary.claude)
        assertTrue(categorizer.calls.isEmpty())
    }

    @Test
    fun nothingToFileOrNoDatabaseMeansNoCall() = runTest {
        val categorizer = FakeCategorizer()
        val claude = CategoriseWithClaudeUseCase(keys, categorizer, ObserveDatabaseUseCase(repository), edit)

        assertNull(claude(listOf("missing")))
        repository.state.value = DatabaseState.NotSelected
        assertNull(claude(listOf("missing")))
        assertTrue(categorizer.calls.isEmpty())
    }

    @Test
    fun confidentAboutNothingFilesNothing() = runTest {
        val summary = commit(FakeCategorizer()).import()

        assertEquals(ClaudeOutcome(filed = 0), summary.claude)
        assertEquals(3, summary.needReview)
    }
}
