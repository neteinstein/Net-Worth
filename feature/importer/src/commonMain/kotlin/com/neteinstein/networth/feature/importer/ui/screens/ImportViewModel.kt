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
import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.SectionTarget
import com.neteinstein.networth.feature.importer.domain.parse.ParseException
import com.neteinstein.networth.feature.importer.domain.parse.ParseFailure
import com.neteinstein.networth.feature.importer.domain.usecase.CommitImportUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.ParseStatementUseCase
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

/** One line of "Earlier imports". */
data class ImportHistoryLine(
    val record: ImportRecord,
    val accountName: String,
    val owners: String,
)

data class ImportUiState(
    val ready: Boolean = false,
    val stage: ImportStage = ImportStage.PICK,
    /** Why the file couldn't be read. Null with [errorUnknown] false = no error. */
    val error: ParseFailure? = null,
    val errorUnknown: Boolean = false,
    val fileName: String? = null,
    val sections: List<SectionUi> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val members: List<Member> = emptyList(),
    val summary: ImportSummary? = null,
    /** Periods of the earlier imports that the duplicates came from. */
    val overlaps: List<Pair<LocalDate, LocalDate>> = emptyList(),
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
    val summary: ImportSummary? = null,
    val error: ParseFailure? = null,
    val errorUnknown: Boolean = false,
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
        val overlaps =
            s.summary?.overlappedImportIds.orEmpty().mapNotNull { id -> db.imports.firstOrNull { it.id == id } }
                .map { it.periodStart to it.periodEnd }.distinct()
        return ImportUiState(
            ready = true,
            stage = s.stage,
            error = s.error,
            errorUnknown = s.errorUnknown,
            fileName = s.fileName,
            sections = sections,
            accounts = db.accounts,
            members = db.members,
            summary = s.summary,
            overlaps = overlaps,
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

    fun onFilePicked(
        name: String,
        bytes: ByteArray,
    ) {
        session.value = ImportSession(stage = ImportStage.READING, fileName = name)
        viewModelScope.launch {
            val statement =
                try {
                    parse(name, bytes)
                } catch (e: ParseException) {
                    session.value = ImportSession(fileName = name, error = e.failure)
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    session.value = ImportSession(fileName = name, errorUnknown = true)
                    return@launch
                }
            val db = (database.value as? DatabaseState.Ready)?.database ?: return@launch
            val choices =
                statement.sections.mapIndexed { index, section ->
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
            session.value = ImportSession(stage = ImportStage.CONFIRM, fileName = name, statement = statement, choices = choices)
            val everyMatched = choices.all { it.accountId != null }
            if (!db.settings.askAccountOnImport && everyMatched) onImport()
        }
    }

    fun onPickFailed() {
        session.value = ImportSession(errorUnknown = true)
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
        session.update { it.copy(stage = ImportStage.IMPORTING) }
        viewModelScope.launch {
            try {
                val summary = commit(ImportRequest(statement, targets))
                session.update { it.copy(stage = ImportStage.DONE, summary = summary) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                session.update { it.copy(stage = ImportStage.CONFIRM, errorUnknown = true) }
            }
        }
    }

    fun onStartOver() {
        session.value = ImportSession()
    }

    fun onDismissError() {
        session.update { it.copy(error = null, errorUnknown = false) }
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

/** True when a statement section is a near-certain match, i.e. the file named the account. */
internal val SectionUi.isMatched: Boolean get() = matchedAccountId != null && matchedAccountId == choice.accountId
