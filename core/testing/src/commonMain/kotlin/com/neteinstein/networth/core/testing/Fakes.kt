package com.neteinstein.networth.core.testing

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseFile
import com.neteinstein.networth.core.domain.repository.DatabaseFileName
import com.neteinstein.networth.core.domain.repository.DatabaseFolder
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.FamilyDatabaseRepository
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.repository.NewDatabaseFile
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.core.domain.seed.HouseholdSetup
import com.neteinstein.networth.core.domain.seed.toDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

val testUser = UserProfile("1", "pedro@example.com", "Pedro Vicente")

class FakeAuthRepository(signedIn: Boolean = false) : AuthRepository {
    override val state = MutableStateFlow(if (signedIn) AuthState.SignedIn(testUser) else AuthState.SignedOut)

    /** What the next sign-in does (return a user or throw). */
    var signInResult: () -> UserProfile = { testUser }

    /** What the Google Picker returns next. */
    var pickResult: () -> String? = { "picked-file" }

    /** What the Google Picker for folders returns next. */
    var pickFolderResult: () -> String? = { "picked-folder" }

    override suspend fun restore() = Unit

    override suspend fun signIn(): UserProfile = signInResult().also { state.value = AuthState.SignedIn(it) }

    override suspend fun pickDatabaseFile(): String? = pickResult()

    override suspend fun pickDatabaseFolder(): String? = pickFolderResult()

    override suspend fun signOut() {
        state.value = AuthState.SignedOut
    }
}

class FakeDatabaseRepository(initial: DatabaseState = DatabaseState.NotSelected) : FamilyDatabaseRepository {
    override val state = MutableStateFlow(initial)
    val calls = mutableListOf<String>()
    var failure: Exception? = null
    var now = 1_000L

    val database: FamilyDatabase get() = (state.value as DatabaseState.Ready).database

    private fun check() {
        failure?.let { throw it }
    }

    override suspend fun restore() {
        calls += "restore"
    }

    override suspend fun createNew(setup: HouseholdSetup, file: NewDatabaseFile): DatabaseFile {
        check()
        calls += buildString {
            append("create:${setup.householdName}")
            file.name?.let { append(" name:$it") }
            file.folderId?.let { append(" in:$it") }
        }
        val created = DatabaseFile("new-file", file.name ?: DatabaseFileName.forHousehold(setup.householdName))
        state.value = DatabaseState.Ready(setup.toDatabase(SequentialIds(), now, testUser.email), created, SyncStatus.SYNCED)
        return created
    }

    override suspend fun folder(folderId: String): DatabaseFolder {
        check()
        return DatabaseFolder(folderId, "Folder $folderId")
    }

    override suspend fun open(fileId: String) {
        check()
        calls += "open:$fileId"
    }

    override suspend fun edit(transform: (database: FamilyDatabase, now: Long) -> FamilyDatabase) {
        calls += "edit"
        val ready = state.value as? DatabaseState.Ready ?: return
        state.value = ready.copy(database = transform(ready.database, now++), sync = SyncStatus.PENDING)
    }

    override suspend fun sync() {
        calls += "sync"
    }

    override suspend fun share(email: String) {
        check()
        calls += "share:$email"
    }

    override suspend fun close() {
        calls += "close"
        state.value = DatabaseState.NotSelected
    }

    companion object {
        /** A ready repository holding [database]. */
        fun ready(database: FamilyDatabase) =
            FakeDatabaseRepository(
                DatabaseState.Ready(
                    database,
                    DatabaseFile("file-1", "Net Worth – Test.networth.json", "https://drive/f"),
                    SyncStatus.SYNCED,
                ),
            )
    }
}

class SequentialIds(private val prefix: String = "id") : IdGenerator {
    private var next = 1

    override fun newId(): String = "$prefix-${next++}"
}

class FixedClock(
    private val date: LocalDate = LocalDate(2026, 10, 1),
    private var millis: Long = 1_000,
) : AppClock {
    override fun nowMillis(): Long = millis++

    override fun today(): LocalDate = date
}

/** The family from the design canvas: two adults, three kids, default accounts and categories. */
fun designFamily(): FamilyDatabase = HouseholdSetup(
    householdName = "Vicente",
    adults = listOf("Pedro", "Maria"),
    children = listOf("Inês", "Tomás", "Rita"),
).toDatabase(SequentialIds(), 0, testUser.email)
