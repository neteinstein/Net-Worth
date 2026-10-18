package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseError
import com.neteinstein.networth.core.domain.repository.DatabaseException
import com.neteinstein.networth.core.domain.repository.DatabaseFile
import com.neteinstein.networth.core.domain.repository.DatabaseFileName
import com.neteinstein.networth.core.domain.repository.DatabaseFolder
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.FamilyDatabaseRepository
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.repository.LocalStore
import com.neteinstein.networth.core.domain.repository.NewDatabaseFile
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.core.domain.seed.HouseholdSetup
import com.neteinstein.networth.core.domain.seed.toDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The family database as one JSON file in Google Drive, with a local copy so the app opens
 * instantly and keeps working offline.
 *
 * Edits apply to the local copy immediately and are uploaded [uploadDelayMs] later (so a burst of
 * edits is one upload). Before every upload the Drive file's `version` is checked; if another
 * device changed it, its copy is downloaded and merged record by record ([DatabaseMerger]) first.
 * Drive has no compare-and-swap, so two uploads landing within the same second can still race;
 * the merge on the next sync limits that to one round of edits.
 */
class DriveFamilyDatabaseRepository(
    private val remote: DatabaseRemote,
    private val auth: AuthRepository,
    store: LocalStore,
    private val clock: AppClock,
    private val ids: IdGenerator,
    private val scope: CoroutineScope,
    private val uploadDelayMs: Long = 2_000,
) : FamilyDatabaseRepository {
    private val cache = DatabaseCache(store)
    private val mutableState = MutableStateFlow<DatabaseState>(DatabaseState.Loading)
    override val state: StateFlow<DatabaseState> = mutableState.asStateFlow()

    /** Guards [current]; held only for local work, never across network calls. */
    private val localLock = Mutex()

    /** Serialises syncs. */
    private val syncLock = Mutex()
    private var current: CachedDatabase? = null
    private var uploadJob: Job? = null

    private val userEmail: String
        get() = (auth.state.value as? AuthState.SignedIn)?.user?.email ?: "unknown"

    override suspend fun restore() {
        val cached = cache.load()
        localLock.withLock { current = cached }
        if (cached == null) {
            mutableState.value = DatabaseState.NotSelected
            return
        }
        publish(cached, if (cached.pending) SyncStatus.PENDING else SyncStatus.SYNCED)
        scope.launch { sync() }
    }

    override suspend fun createNew(setup: HouseholdSetup, file: NewDatabaseFile): DatabaseFile {
        val database = setup.toDatabase(ids, clock.nowMillis(), userEmail)
        val name = file.name?.let(DatabaseFileName::custom) ?: DatabaseFileName.forHousehold(setup.householdName)
        val info = guard(creating = true) { remote.create(name, FamilyDatabaseCodec.encode(database), file.folderId) }
        val cached = CachedDatabase(info.id, info.name, info.webViewLink, info.version, pending = false, database = database)
        replaceCurrent(cached)
        publish(cached, SyncStatus.SYNCED)
        return cached.file
    }

    override suspend fun folder(folderId: String): DatabaseFolder = guard { DatabaseFolder(folderId, remote.info(folderId).name) }

    override suspend fun open(fileId: String) {
        val info = guard { remote.info(fileId) }
        val downloaded = FamilyDatabaseCodec.decode(guard { remote.download(fileId) })
        val previous = localLock.withLock { current }
        // Reopening the file this device already has: keep any edits that never made it to Drive.
        val keepLocal = previous != null && previous.fileId == fileId && previous.pending
        val database = if (keepLocal) DatabaseMerger.merge(previous.database, downloaded) else downloaded
        val cached = CachedDatabase(info.id, info.name, info.webViewLink, info.version, pending = keepLocal, database = database)
        replaceCurrent(cached)
        publish(cached, if (keepLocal) SyncStatus.PENDING else SyncStatus.SYNCED)
        if (keepLocal) scheduleUpload(immediately = true)
    }

    override suspend fun edit(transform: (database: FamilyDatabase, now: Long) -> FamilyDatabase) {
        val updated = localLock.withLock {
            val cached = current ?: return
            val now = clock.nowMillis()
            val database = transform(cached.database, now).copy(updatedAt = now, updatedBy = userEmail)
            cached.copy(database = database, pending = true).also {
                current = it
                cache.save(it)
            }
        }
        publish(updated, SyncStatus.PENDING)
        scheduleUpload()
    }

    override suspend fun sync() {
        syncLock.withLock { syncLocked() }
    }

    private suspend fun syncLocked() {
        val snapshot = localLock.withLock { current } ?: return
        publish(snapshot, SyncStatus.SYNCING)
        try {
            var info = remote.info(snapshot.fileId)
            var database = snapshot.database
            var mustUpload = snapshot.pending
            if (info.version != snapshot.remoteVersion) {
                val remoteDatabase = FamilyDatabaseCodec.decode(remote.download(snapshot.fileId))
                database = DatabaseMerger.merge(database, remoteDatabase)
                mustUpload = database != DatabaseMerger.normalize(remoteDatabase)
            }
            if (mustUpload) info = remote.update(snapshot.fileId, FamilyDatabaseCodec.encode(database))
            val synced = localLock.withLock {
                val latest = current ?: return@withLock null
                if (latest.fileId != snapshot.fileId) return@withLock null
                // Edits made while we were talking to Drive are merged in and stay pending.
                val editedMeanwhile = latest.database !== snapshot.database
                val merged = if (editedMeanwhile) DatabaseMerger.merge(latest.database, database) else database
                latest.copy(database = merged, remoteVersion = info.version, pending = editedMeanwhile, fileName = info.name).also {
                    current = it
                    cache.save(it)
                }
            } ?: return
            publish(synced, if (synced.pending) SyncStatus.PENDING else SyncStatus.SYNCED)
            if (synced.pending) scheduleUpload()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            localLock.withLock { current }?.let { publish(it, SyncStatus.OFFLINE) }
        }
    }

    override suspend fun share(email: String) {
        val fileId = localLock.withLock { current?.fileId } ?: return
        guard { remote.share(fileId, email) }
    }

    override suspend fun close() {
        uploadJob?.cancel()
        localLock.withLock { current = null }
        cache.clear()
        mutableState.value = DatabaseState.NotSelected
    }

    private suspend fun replaceCurrent(cached: CachedDatabase) {
        uploadJob?.cancel()
        localLock.withLock {
            current = cached
            cache.save(cached)
        }
    }

    private fun scheduleUpload(immediately: Boolean = false) {
        uploadJob?.cancel()
        uploadJob = scope.launch {
            if (!immediately) delay(uploadDelayMs)
            sync()
        }
    }

    private fun publish(cached: CachedDatabase, status: SyncStatus) {
        mutableState.value = DatabaseState.Ready(cached.database, cached.file, status)
    }

    private val CachedDatabase.file get() = DatabaseFile(fileId, fileName, webViewLink)

    /**
     * Maps remote failures to [DatabaseException] so the UI can explain them. When [creating], a
     * refusal is about the user's own Drive, not someone else's file, so it can't be "not shared".
     */
    private suspend fun <T> guard(creating: Boolean = false, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: DatabaseException) {
        throw e
    } catch (e: Exception) {
        val error = remote.errorOf(e)
        throw DatabaseException(if (creating && error == DatabaseError.NOT_ACCESSIBLE) DatabaseError.NO_DRIVE_PERMISSION else error, e)
    }
}
