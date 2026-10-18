package com.neteinstein.networth.feature.importer.domain.usecase

import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.CategorizerException
import com.neteinstein.networth.core.domain.repository.ClaudeKeyRepository
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.repository.MovementToFile
import com.neteinstein.networth.core.domain.repository.TransactionCategorizer
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.importer.domain.ImportPlanner
import com.neteinstein.networth.feature.importer.domain.ReviewQueue
import com.neteinstein.networth.feature.importer.domain.model.ClaudeOutcome
import com.neteinstein.networth.feature.importer.domain.model.ImportRequest
import com.neteinstein.networth.feature.importer.domain.model.ImportSummary
import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.ReviewChoice
import com.neteinstein.networth.feature.importer.domain.parse.StatementParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Reads a bank file off the main thread. Throws [com.neteinstein.networth.feature.importer.domain.parse.ParseException]. */
class ParseStatementUseCase(
    private val parser: StatementParser,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend operator fun invoke(
        fileName: String,
        bytes: ByteArray,
    ): ParsedStatement = withContext(dispatcher) { parser.parse(fileName, bytes) }
}

/**
 * Writes a parsed statement into the family database and reports what happened. When this device has
 * a Claude key, Claude then files what the merchant rules couldn't (see [CategoriseWithClaudeUseCase]).
 */
class CommitImportUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
    private val clock: AppClock,
    private val auth: ObserveAuthUseCase,
    private val categoriseWithClaude: CategoriseWithClaudeUseCase,
) {
    suspend operator fun invoke(request: ImportRequest): ImportSummary {
        val user = (auth().value as? AuthState.SignedIn)?.user
        val importedBy = user?.displayName ?: user?.email ?: ""
        val planner = ImportPlanner(ids)
        val today = clock.today()
        var summary: ImportSummary? = null
        var addedIds: List<String> = emptyList()
        edit { database, now ->
            val result = planner.apply(database, request, now, today, importedBy)
            summary = result.summary
            addedIds = result.addedIds
            result.database
        }
        val imported = checkNotNull(summary) { "No database is open" }
        val claude = categoriseWithClaude(addedIds) ?: return imported
        return imported.copy(
            autoCategorised = imported.autoCategorised + claude.filed,
            needReview = imported.needReview - claude.filed,
            claude = claude,
        )
    }
}

/**
 * Asks Claude to file the given transactions that still need review, with the key saved on this
 * device. Claude's choices are applied only to transactions that are still uncategorised and only
 * with categories that still exist; nothing is learned as a rule. Returns null when there's no key
 * or nothing to file. A failure is reported in the outcome, never thrown: the import itself is done.
 */
class CategoriseWithClaudeUseCase(
    private val keys: ClaudeKeyRepository,
    private val categorizer: TransactionCategorizer,
    private val observeDatabase: ObserveDatabaseUseCase,
    private val edit: EditDatabaseUseCase,
) {
    suspend operator fun invoke(transactionIds: Collection<String>): ClaudeOutcome? {
        val key = keys.observeApiKey().first() ?: return null
        val database = (observeDatabase().value as? DatabaseState.Ready)?.database ?: return null
        val wanted = transactionIds.toSet()
        val waiting = database.transactions.filter { it.id in wanted && it.needsReview }
        if (waiting.isEmpty() || database.categories.isEmpty()) return null
        val guesses =
            try {
                categorizer.categorize(
                    key,
                    waiting.map { MovementToFile(it.id, it.description, it.amountMinor) },
                    database.categories,
                    database.settings.currencyCode,
                )
            } catch (e: CategorizerException) {
                return ClaudeOutcome(filed = 0, failure = e.failure)
            }
        if (guesses.isEmpty()) return ClaudeOutcome(filed = 0)
        var filed = 0
        edit { current, now ->
            filed = 0
            val categories = current.categories.associateBy { it.id }
            current.copy(
                transactions =
                    current.transactions.map { tx ->
                        val guess = guesses[tx.id]
                        val category = guess?.let { categories[it.categoryId] }
                        if (category == null || !tx.needsReview) return@map tx
                        filed++
                        tx.copy(
                            categoryId = category.id,
                            subcategoryId = guess.subcategoryId?.takeIf { id -> category.subcategories.any { it.id == id } },
                            updatedAt = now,
                        )
                    },
            )
        }
        return ClaudeOutcome(filed)
    }
}

/** Saves Review & learn choices; returns how many transactions got a category (learned rules included). */
class SaveReviewUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(choices: List<ReviewChoice>): Int {
        var filed = 0
        edit { database, now ->
            val (updated, count) = ReviewQueue.apply(database, choices, ids, now)
            filed = count
            updated
        }
        return filed
    }
}
