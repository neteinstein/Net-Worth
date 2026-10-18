package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.repository.DatabaseError
import com.neteinstein.networth.core.google.DriveClient
import com.neteinstein.networth.core.google.DriveException
import com.neteinstein.networth.core.google.DriveProblem

/** Where the database file lives; Google Drive in the app, a fake in tests. */
interface DatabaseRemote {
    data class FileInfo(
        val id: String,
        val name: String,
        val version: String?,
        val webViewLink: String?,
    )

    /** Creates [name] in [folderId], or in the root of My Drive when it is null. */
    suspend fun create(name: String, content: String, folderId: String? = null): FileInfo

    suspend fun info(fileId: String): FileInfo

    suspend fun download(fileId: String): String

    suspend fun update(fileId: String, content: String): FileInfo

    suspend fun share(fileId: String, email: String)

    /** What a failure means to the user; [DatabaseError.UNKNOWN] for transient problems. */
    fun errorOf(error: Throwable): DatabaseError
}

class DriveDatabaseRemote(private val drive: DriveClient) : DatabaseRemote {
    override suspend fun create(name: String, content: String, folderId: String?) = drive.create(name, content, folderId).toInfo()

    override suspend fun info(fileId: String): DatabaseRemote.FileInfo {
        val metadata = drive.metadata(fileId)
        if (metadata.trashed) throw DriveException(404, "File is in the trash")
        return metadata.toInfo()
    }

    override suspend fun download(fileId: String) = drive.download(fileId)

    override suspend fun update(fileId: String, content: String) = drive.update(fileId, content).toInfo()

    override suspend fun share(fileId: String, email: String) = drive.shareWith(fileId, email)

    override fun errorOf(error: Throwable) = when ((error as? DriveException)?.problem) {
        DriveProblem.API_DISABLED -> DatabaseError.DRIVE_API_DISABLED
        DriveProblem.SCOPE_MISSING -> DatabaseError.NO_DRIVE_PERMISSION
        DriveProblem.STORAGE_FULL -> DatabaseError.DRIVE_FULL
        DriveProblem.NO_ACCESS -> DatabaseError.NOT_ACCESSIBLE
        DriveProblem.OTHER, null -> DatabaseError.UNKNOWN
    }

    private fun com.neteinstein.networth.core.google.DriveFileMetadata.toInfo() = DatabaseRemote.FileInfo(id, name, version, webViewLink)
}
