package com.neteinstein.networth.feature.importer.domain.usecase

import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.feature.importer.domain.ImportPlanner
import com.neteinstein.networth.feature.importer.domain.ReviewQueue
import com.neteinstein.networth.feature.importer.domain.model.ImportRequest
import com.neteinstein.networth.feature.importer.domain.model.ImportSummary
import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.ReviewChoice
import com.neteinstein.networth.feature.importer.domain.parse.StatementParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

/** Writes a parsed statement into the family database and reports what happened. */
class CommitImportUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
    private val clock: AppClock,
    private val auth: ObserveAuthUseCase,
) {
    suspend operator fun invoke(request: ImportRequest): ImportSummary {
        val user = (auth().value as? AuthState.SignedIn)?.user
        val importedBy = user?.displayName ?: user?.email ?: ""
        val planner = ImportPlanner(ids)
        val today = clock.today()
        var summary: ImportSummary? = null
        edit { database, now ->
            val result = planner.apply(database, request, now, today, importedBy)
            summary = result.summary
            result.database
        }
        return checkNotNull(summary) { "No database is open" }
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
