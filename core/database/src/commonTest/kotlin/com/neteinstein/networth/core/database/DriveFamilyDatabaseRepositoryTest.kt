package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.repository.DatabaseError
import com.neteinstein.networth.core.domain.repository.DatabaseException
import com.neteinstein.networth.core.domain.repository.DatabaseFolder
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.InMemoryLocalStore
import com.neteinstein.networth.core.domain.repository.NewDatabaseFile
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.core.domain.seed.HouseholdSetup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Repositories launch their uploads into the test scope itself so advanceUntilIdle() drives them.
@OptIn(ExperimentalCoroutinesApi::class)
class DriveFamilyDatabaseRepositoryTest {
    private val remote = FakeRemote()
    private val store = InMemoryLocalStore()
    private val setup = HouseholdSetup("Vicente", listOf("Pedro", "Maria"), listOf("Inês"))

    private fun TestScope.repository(
        scope: CoroutineScope = this,
        ids: SequentialIds = SequentialIds(),
        email: String = "pedro@example.com",
        clock: FakeClock = FakeClock(),
    ) = DriveFamilyDatabaseRepository(remote, FakeAuth(email), store, clock, ids, scope, uploadDelayMs = 2_000)

    private val DriveFamilyDatabaseRepository.ready get() = assertIs<DatabaseState.Ready>(state.value)

    @Test
    fun restoreWithoutCacheAsksForADatabase() = runTest {
        val repo = repository()
        assertEquals(DatabaseState.Loading, repo.state.value)

        repo.restore()

        assertEquals(DatabaseState.NotSelected, repo.state.value)
    }

    @Test
    fun createNewUploadsSeededDatabase() = runTest {
        val repo = repository()

        val file = repo.createNew(setup)

        assertEquals("Net Worth – Vicente.networth.json", file.name)
        val remoteCopy = FamilyDatabaseCodec.decode(remote.files.getValue(file.id))
        assertEquals("pedro@example.com", remoteCopy.updatedBy)
        assertEquals(remoteCopy, repo.ready.database)
        assertEquals(SyncStatus.SYNCED, repo.ready.sync)
    }

    @Test
    fun editsAreDebouncedIntoOneUpload() = runTest {
        val repo = repository()
        val file = repo.createNew(setup)

        repo.edit { db, now -> db.copy(settings = db.settings.copy(householdName = "A", updatedAt = now)) }
        repo.edit { db, now -> db.copy(settings = db.settings.copy(householdName = "B", updatedAt = now)) }
        assertEquals(SyncStatus.PENDING, repo.ready.sync)
        assertEquals(0, remote.uploads)

        advanceUntilIdle()

        assertEquals(1, remote.uploads)
        assertEquals("B", FamilyDatabaseCodec.decode(remote.files.getValue(file.id)).settings.householdName)
        assertEquals(SyncStatus.SYNCED, repo.ready.sync)
    }

    @Test
    fun syncMergesChangesFromAnotherDevice() = runTest {
        val repo = repository()
        val file = repo.createNew(setup)
        // Maria renames an account on her phone…
        val theirs = FamilyDatabaseCodec.decode(remote.files.getValue(file.id)).let { db ->
            db.copy(accounts = db.accounts.upsert(db.accounts.first().copy(name = "Pedro (BPI)", updatedAt = 9_999)))
        }
        remote.externalWrite(file.id, FamilyDatabaseCodec.encode(theirs))
        // …while Pedro renames the household here.
        repo.edit { db, now -> db.copy(settings = db.settings.copy(householdName = "Os Vicentes", updatedAt = now)) }

        advanceUntilIdle()

        val merged = FamilyDatabaseCodec.decode(remote.files.getValue(file.id))
        assertEquals("Pedro (BPI)", merged.accounts.first { it.id == theirs.accounts.first().id }.name)
        assertEquals("Os Vicentes", merged.settings.householdName)
        assertEquals(merged, repo.ready.database)
    }

    @Test
    fun syncWithoutLocalChangesDownloadsWithoutUploading() = runTest {
        val repo = repository()
        val file = repo.createNew(setup)
        val theirs = FamilyDatabaseCodec.decode(remote.files.getValue(file.id)).let { db ->
            db.copy(settings = db.settings.copy(householdName = "Remote", updatedAt = 99_999))
        }
        remote.externalWrite(file.id, FamilyDatabaseCodec.encode(theirs))

        repo.sync()

        assertEquals("Remote", repo.ready.database.settings.householdName)
        assertEquals(0, remote.uploads)
    }

    @Test
    fun offlineEditsStayPendingAndSurviveRestart() = runTest {
        val file = repository().createNew(setup)
        remote.failWith = FakeRemote.Problem(access = false)
        val repo = repository()
        repo.restore()
        advanceUntilIdle()
        assertEquals(SyncStatus.OFFLINE, repo.ready.sync)

        repo.edit { db, now -> db.copy(settings = db.settings.copy(householdName = "Offline", updatedAt = now)) }
        advanceUntilIdle()
        assertEquals(SyncStatus.OFFLINE, repo.ready.sync)

        // App restarts later with network back.
        remote.failWith = null
        val restarted = repository()
        restarted.restore()
        assertEquals("Offline", restarted.ready.database.settings.householdName)
        advanceUntilIdle()
        assertEquals("Offline", FamilyDatabaseCodec.decode(remote.files.getValue(file.id)).settings.householdName)
        assertEquals(SyncStatus.SYNCED, restarted.ready.sync)
    }

    @Test
    fun openReadsAnotherUsersFileAndKeepsPendingLocalEdits() = runTest {
        val file = repository(email = "maria@example.com").createNew(setup)
        val repo = repository(clock = FakeClock(now = 50_000))

        repo.open(file.id)
        assertEquals("maria@example.com", repo.ready.database.updatedBy)
        assertEquals(file.id, repo.ready.file.id)

        remote.failWith = FakeRemote.Problem(access = false)
        repo.edit { db, now -> db.copy(settings = db.settings.copy(householdName = "Mine", updatedAt = now)) }
        advanceUntilIdle()
        remote.failWith = null

        repo.open(file.id)
        advanceUntilIdle()
        assertEquals("Mine", FamilyDatabaseCodec.decode(remote.files.getValue(file.id)).settings.householdName)
    }

    @Test
    fun openReportsUnreadableFiles() = runTest {
        val repo = repository()
        remote.externalWrite("other", """{"not":"ours"}""")

        assertEquals(DatabaseError.NOT_ACCESSIBLE, assertFailsWith<DatabaseException> { repo.open("missing") }.error)
        assertEquals(DatabaseError.NOT_A_DATABASE, assertFailsWith<DatabaseException> { repo.open("other") }.error)
        remote.failWith = FakeRemote.Problem(access = false)
        assertEquals(DatabaseError.UNKNOWN, assertFailsWith<DatabaseException> { repo.createNew(setup) }.error)
    }

    @Test
    fun createReportsDriveRefusalsAsPermissionProblems() = runTest {
        val repo = repository()

        // Creating in your own Drive: "ask the owner to share it" would make no sense.
        remote.failWith = FakeRemote.Problem(access = true)
        assertEquals(DatabaseError.NO_DRIVE_PERMISSION, assertFailsWith<DatabaseException> { repo.createNew(setup) }.error)
        remote.failWith = FakeRemote.Problem(access = true, error = DatabaseError.DRIVE_API_DISABLED)
        assertEquals(DatabaseError.DRIVE_API_DISABLED, assertFailsWith<DatabaseException> { repo.createNew(setup) }.error)
        remote.failWith = FakeRemote.Problem(access = true, error = DatabaseError.DRIVE_FULL)
        assertEquals(DatabaseError.DRIVE_FULL, assertFailsWith<DatabaseException> { repo.createNew(setup) }.error)
    }

    @Test
    fun shareAndClose() = runTest {
        val repo = repository()
        val file = repo.createNew(setup)

        repo.share("maria@example.com")
        repo.close()

        assertEquals(listOf(file.id to "maria@example.com"), remote.shares)
        assertEquals(DatabaseState.NotSelected, repo.state.value)
        assertTrue(store.read("database-cache.json") == null)
        repo.share("nobody@example.com")
        assertEquals(1, remote.shares.size)
    }

    @Test
    fun createNewUsesTheChosenNameAndFolder() = runTest {
        val repo = repository()

        val file = repo.createNew(setup, NewDatabaseFile(name = "Our money", folderId = "folder-9"))

        assertEquals("Our money.networth.json", file.name)
        assertEquals("folder-9", remote.folders[file.id])
    }

    @Test
    fun createNewPicksNameAndPlaceWhenNotChosen() = runTest {
        val repo = repository()

        val file = repo.createNew(setup, NewDatabaseFile(name = " / "))

        assertEquals("Net Worth – Vicente.networth.json", file.name)
        assertNull(remote.folders[file.id])
    }

    @Test
    fun folderNamesComeFromDrive() = runTest {
        val repo = repository()
        remote.externalWrite("folder-1", "")

        assertEquals(DatabaseFolder("folder-1", "file-folder-1.networth.json"), repo.folder("folder-1"))
        assertEquals(DatabaseError.NOT_ACCESSIBLE, assertFailsWith<DatabaseException> { repo.folder("missing") }.error)
    }
}
