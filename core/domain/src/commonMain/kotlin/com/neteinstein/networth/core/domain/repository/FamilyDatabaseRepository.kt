package com.neteinstein.networth.core.domain.repository

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.seed.HouseholdSetup
import kotlinx.coroutines.flow.StateFlow

/** The Google Drive file that holds the family database. */
data class DatabaseFile(
    val id: String,
    val name: String,
    val webViewLink: String? = null,
)

enum class SyncStatus {
    /** Local copy matches Drive. */
    SYNCED,

    /** An upload or download is in progress. */
    SYNCING,

    /** Local changes are waiting to be uploaded. */
    PENDING,

    /** The last attempt failed (no network, expired session…); changes are kept locally. */
    OFFLINE,
}

sealed interface DatabaseState {
    data object Loading : DatabaseState

    /** Signed in but no database file chosen yet: show "Start fresh / Open a Drive file". */
    data object NotSelected : DatabaseState

    data class Ready(
        val database: FamilyDatabase,
        val file: DatabaseFile,
        val sync: SyncStatus,
    ) : DatabaseState

    data class Failed(val error: DatabaseError) : DatabaseState
}

enum class DatabaseError {
    /** The file is not a Net Worth database (or is corrupt). */
    NOT_A_DATABASE,

    /** The file was written by a newer app version. */
    NEWER_SCHEMA,

    /** Drive refused access or the file no longer exists. */
    NOT_ACCESSIBLE,

    /** The signed-in account didn't give the app access to its files in Google Drive. */
    NO_DRIVE_PERMISSION,

    /** The Google Drive API is not enabled in the app's Google Cloud project (a setup problem). */
    DRIVE_API_DISABLED,

    /** The user's Google Drive is out of storage. */
    DRIVE_FULL,

    /** Network or other unexpected failure. */
    UNKNOWN,
}

/** Thrown by [FamilyDatabaseRepository.open] and [FamilyDatabaseRepository.createNew]. */
class DatabaseException(
    val error: DatabaseError,
    cause: Throwable? = null,
) : Exception(error.name, cause)

/**
 * The family database: one JSON file in Google Drive, cached locally and synced in the background.
 */
interface FamilyDatabaseRepository {
    val state: StateFlow<DatabaseState>

    /** Reopens the last used file from the local cache (then syncs), or reports [DatabaseState.NotSelected]. */
    suspend fun restore()

    /** Creates a new database file in the signed-in user's Drive and opens it. */
    suspend fun createNew(setup: HouseholdSetup): DatabaseFile

    /** Opens an existing database file (e.g. one picked with the Google Picker). */
    suspend fun open(fileId: String)

    /** Applies a change locally right away; it is uploaded shortly after. [now] is epoch millis. */
    suspend fun edit(transform: (database: FamilyDatabase, now: Long) -> FamilyDatabase)

    /** Pulls remote changes and pushes local ones now. */
    suspend fun sync()

    /** Gives [email] edit access to the database file. */
    suspend fun share(email: String)

    /** Forgets the current file (it stays in Drive). */
    suspend fun close()
}
