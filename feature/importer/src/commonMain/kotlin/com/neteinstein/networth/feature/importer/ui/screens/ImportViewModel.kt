package com.neteinstein.networth.feature.importer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.ImportRecord
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.AddMemberUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.importer.domain.AccountMatcher
import com.neteinstein.networth.feature.importer.domain.Coverage
import com.neteinstein.networth.feature.importer.domain.model.AccountCoverage
import com.neteinstein.networth.feature.importer.domain.model.ImportRequest
import com.neteinstein.networth.feature.importer.domain.model.ImportSummary
import com.neteinstein.networth.feature.importer.domain.model.ParsedSection
import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.SectionTarget
import com.neteinstein.networth.feature.importer.domain.parse.ParseException
import com.neteinstein.networth.feature.importer.domain.parse.ParseFailure
import com.neteinstein.networth.feature.importer.domain.parse.Values
import com.neteinstein.networth.feature.importer.domain.usecase.CommitImportUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.ParseStatementUseCase
import com.neteinstein.networth.feature.importer.ui.components.PickedFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

enum class ImportStage { PICK, READING, CONFIRM, IMPORTING, DONE }

/** The account one section of the file goes to, as chosen on screen. [accountId] null = a new account. */
data class SectionChoice(
    val index: Int,
    val accountId: String?,
    val newName: String,
    val newType: AccountType,
    val ownerIds: List<String>,
    val include: Boolean = true,
)

data class SectionUi(
    val index: Int,
    val label: String,
    val accountNumber: String?,
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?,
    val rows: Int,
    val matchedAccountId: String?,
    val choice: SectionChoice,
)

/** A file that couldn't be read or imported, and why; [failure] null = an unknown error. */
data class FileError(
    val fileName: String?,
    val failure: ParseFailure?,
)

/** One imported file, and the periods of the earlier imports its duplicates came from. */
data class ImportResult(
    val summary: ImportSummary,
    val overlaps: List<Pair<LocalDate, LocalDate>>,
)

/** One line of "Earlier imports". */
data class ImportHistoryLine(
    val record: ImportRecord,
    val accountName: String,
    val owners: String,
)

data class ImportUiState(
    val ready: Boolean = false,
    val stage: ImportStage = ImportStage.PICK,
    val errors: List<FileError> = emptyList(),
    val fileName: String? = null,
    /** Which of the files picked together is being read or confirmed (1-based), and how many there are. */
    val fileNumber: Int = 0,
    val fileCount: Int = 0,
    /** More picked files wait after this one, so the choices made here can carry over. */
    val hasMoreFiles: Boolean = false,
    /** Reuse this file's accounts, owners and skipped sections for the next files from the same accounts. */
    val reuseChoices: Boolean = true,
    val sections: List<SectionUi> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val members: List<Member> = emptyList(),
    /** The files imported so far from the last pick, in order. */
    val results: List<ImportResult> = emptyList(),
    val coverage: List<AccountCoverage> = emptyList(),
    val history: List<ImportHistoryLine> = emptyList(),
    val pendingReview: Int = 0,
    val currencyCode: String = "EUR",
) {
    val canImport: Boolean
        get() = stage == ImportStage.CONFIRM &&
            sections.any { it.choice.include } &&
            sections.filter { it.choice.include }.all { it.choice.accountId != null || it.choice.newName.isNotBlank() }
}

private data class ImportSession(
    val stage: ImportStage = ImportStage.PICK,
    val fileName: String? = null,
    val statement: ParsedStatement? = null,
    val choices: List<SectionChoice> = emptyList(),
    /** Files picked together that are still to be read after the current one. */
    val queue: List<PickedFile> = emptyList(),
    val fileCount: Int = 0,
    val summaries: List<ImportSummary> = emptyList(),
    val errors: List<FileError> = emptyList(),
    val reuseChoices: Boolean = true,
    /** Choices confirmed for earlier files of this pick, by account key (see [accountKey]). */
    val remembered: Map<String, SectionChoice> = emptyMap(),
)

class ImportViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    private val parse: ParseStatementUseCase,
    private val commit: CommitImportUseCase,
    private val addMember: AddMemberUseCase,
    private val clock: AppClock,
) : ViewModel() {
    private val session = MutableStateFlow(ImportSession())
    private val database = observeDatabase()

    val state: StateFlow<ImportUiState> =
        combine(database, session) { db, s ->
            val ready = db as? DatabaseState.Ready ?: return@combine ImportUiState()
            build(ready.database, s)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImportUiState())

    private fun build(
        db: FamilyDatabase,
        s: ImportSession,
    ): ImportUiState {
        val statement = s.statement
        val sections =
            statement?.sections?.mapIndexedNotNull { index, section ->
                val choice = s.choices.firstOrNull { it.index == index } ?: return@mapIndexedNotNull null
                SectionUi(
                    index = index,
                    label = section.label,
                    accountNumber = section.accountNumber,
                    periodStart = section.start,
                    periodEnd = section.end,
                    rows = section.rows.size,
                    matchedAccountId = AccountMatcher.match(db, section)?.id,
                    choice = choice,
                )
            }.orEmpty()
        val names = db.accounts.associate { it.id to it.name }
        val results =
            s.summaries.map { summary ->
                val overlaps =
                    summary.overlappedImportIds.mapNotNull { id -> db.imports.firstOrNull { it.id == id } }
                        .map { it.periodStart to it.periodEnd }.distinct()
                ImportResult(summary, overlaps)
            }
        return ImportUiState(
            ready = true,
            stage = s.stage,
            errors = s.errors,
            fileName = s.fileName,
            fileNumber = if (s.stage == ImportStage.DONE) 0 else s.fileCount - s.queue.size,
            fileCount = s.fileCount,
            hasMoreFiles = s.queue.isNotEmpty(),
            reuseChoices = s.reuseChoices,
            sections = sections,
            accounts = db.accounts,
            members = db.members,
            results = results,
            coverage = Coverage.of(db, CalendarMonth.of(clock.today())),
            history =
                db.imports.sortedByDescending { it.importedAt }.take(HISTORY).map { record ->
                    val account = db.account(record.accountId)
                    ImportHistoryLine(
                        record = record,
                        accountName = names[record.accountId].orEmpty(),
                        owners = account?.ownerIds.orEmpty().mapNotNull { db.member(it)?.name }.joinToString(" & "),
                    )
                },
            pendingReview = db.transactions.count { it.needsReview },
            currencyCode = db.settings.currencyCode,
        )
    }

    /** Reads the picked files one after the other; each is confirmed (or imported straight away) in turn. */
    fun onFilesPicked(files: List<PickedFile>) {
        if (files.isEmpty()) return
        session.value = ImportSession(queue = files, fileCount = files.size)
        readNext()
    }

    fun onPickFailed() {
        session.value = ImportSession(errors = listOf(FileError(fileName = null, failure = null)))
    }

    /** Leaves the file being confirmed out and goes on with the next one. */
    fun onSkipFile() {
        if (session.value.stage == ImportStage.CONFIRM) readNext()
    }

    private fun readNext() {
        val current = session.value
        val file = current.queue.firstOrNull()
        if (file == null) {
            session.update {
                it.copy(
                    stage = if (it.summaries.isEmpty()) ImportStage.PICK else ImportStage.DONE,
                    fileName = null,
                    statement = null,
                    choices = emptyList(),
                )
            }
            return
        }
        session.value =
            current.copy(
                stage = ImportStage.READING,
                fileName = file.name,
                statement = null,
                choices = emptyList(),
                queue = current.queue.drop(1),
            )
        viewModelScope.launch {
            val statement =
                try {
                    parse(file.name, file.bytes)
                } catch (e: ParseException) {
                    failed(FileError(file.name, e.failure))
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed(FileError(file.name, failure = null))
                    return@launch
                }
            val db = (database.value as? DatabaseState.Ready)?.database ?: return@launch
            val remembered = session.value.remembered
            val known = statement.sections.map { remembered[accountKey(statement, it)] }
            val choices =
                statement.sections.mapIndexed { index, section ->
                    known[index]?.let { return@mapIndexed it.copy(index = index) }
                    val match = AccountMatcher.match(db, section)
                    SectionChoice(
                        index = index,
                        accountId = match?.id,
                        newName = section.label,
                        newType = section.suggestedType,
                        ownerIds = match?.ownerIds.orEmpty(),
                        // Empty sections (e.g. a savings account with no movements) are only worth their balance.
                        include = section.rows.isNotEmpty() || match != null || section.closingBalanceMinor != null,
                    )
                }
            session.update { it.copy(stage = ImportStage.CONFIRM, statement = statement, choices = choices) }
            when {
                // Every account was already confirmed for an earlier file of this pick: no need to ask again.
                known.all { it != null } -> if (choices.any { it.include }) onImport() else readNext()
                !db.settings.askAccountOnImport && choices.all { it.accountId != null } -> onImport()
            }
        }
    }

    private fun failed(error: FileError) {
        session.update { it.copy(errors = it.errors + error) }
        readNext()
    }

    fun onReuseChoices(reuse: Boolean) {
        session.update { it.copy(reuseChoices = reuse) }
    }

    fun onAccountSelected(
        index: Int,
        accountId: String?,
    ) = updateChoice(index) { choice ->
        val account = accountId?.let { id -> state.value.accounts.firstOrNull { it.id == id } }
        choice.copy(accountId = accountId, ownerIds = account?.ownerIds ?: choice.ownerIds)
    }

    fun onNewAccountName(
        index: Int,
        name: String,
    ) = updateChoice(index) { it.copy(newName = name) }

    fun onNewAccountType(
        index: Int,
        type: AccountType,
    ) = updateChoice(index) { it.copy(newType = type) }

    fun onIncludeSection(
        index: Int,
        include: Boolean,
    ) = updateChoice(index) { it.copy(include = include) }

    fun onOwnerToggled(
        index: Int,
        memberId: String,
    ) = updateChoice(index) { choice ->
        choice.copy(ownerIds = if (memberId in choice.ownerIds) choice.ownerIds - memberId else choice.ownerIds + memberId)
    }

    /** Adds someone to the household and makes them an owner of this section's account straight away. */
    fun onAddPerson(
        index: Int,
        name: String,
        role: MemberRole,
    ) {
        viewModelScope.launch {
            val member = addMember(name, role)
            updateChoice(index) { it.copy(ownerIds = it.ownerIds + member.id) }
        }
    }

    fun onImport() {
        val current = session.value
        val statement = current.statement ?: return
        val targets =
            current.choices.filter { it.include }.map { choice ->
                SectionTarget(choice.index, choice.accountId, choice.newName, choice.newType, choice.ownerIds)
            }
        if (targets.isEmpty()) return
        // A retry replaces the error of the attempt before.
        session.update { s -> s.copy(stage = ImportStage.IMPORTING, errors = s.errors.filterNot { it.fileName == s.fileName }) }
        viewModelScope.launch {
            try {
                val summary = commit(ImportRequest(statement, targets))
                session.update { s ->
                    val remembered =
                        if (s.reuseChoices) {
                            s.remembered +
                                s.choices.associate { choice ->
                                    val accountId = summary.sectionAccountIds[choice.index] ?: choice.accountId
                                    accountKey(statement, statement.sections[choice.index]) to choice.copy(accountId = accountId)
                                }
                        } else {
                            s.remembered
                        }
                    s.copy(summaries = s.summaries + summary, remembered = remembered)
                }
                readNext()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                session.update { it.copy(stage = ImportStage.CONFIRM, errors = it.errors + FileError(it.fileName, failure = null)) }
            }
        }
    }

    fun onStartOver() {
        session.value = ImportSession()
    }

    fun onDismissError() {
        session.update { it.copy(errors = emptyList()) }
    }

    private fun updateChoice(
        index: Int,
        transform: (SectionChoice) -> SectionChoice,
    ) {
        session.update { s -> s.copy(choices = s.choices.map { if (it.index == index) transform(it) else it }) }
    }

    private companion object {
        const val HISTORY = 8
    }
}

/** What identifies a section's account across files: its IBAN or number, else the bank and the section's name. */
internal fun accountKey(
    statement: ParsedStatement,
    section: ParsedSection,
): String =
    section.iban?.uppercase()?.let(Values::digits)?.takeIf { it.isNotEmpty() }
        ?: section.accountNumber?.let(Values::digits)?.takeIf { it.isNotEmpty() }
        ?: "${statement.bank.orEmpty()}|${statement.format}|${section.label}".uppercase()

/** True when a statement section is a near-certain match, i.e. the file named the account. */
internal val SectionUi.isMatched: Boolean get() = matchedAccountId != null && matchedAccountId == choice.accountId
