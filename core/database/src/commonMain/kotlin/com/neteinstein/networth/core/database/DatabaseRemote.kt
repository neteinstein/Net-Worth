package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.google.DriveClient
import com.neteinstein.networth.core.google.DriveException

/** Where the database file lives; Google Drive in the app, a fake in tests. */
interface DatabaseRemote {
    data class FileInfo(
        val id: String,
        val name: String,
        val version: String?,
        val webViewLink: String?,
    )

    suspend fun create(name: String, content: String): FileInfo

    suspend fun info(fileId: String): FileInfo

    suspend fun download(fileId: String): String

    suspend fun update(fileId: String, content: String): FileInfo

    suspend fun share(fileId: String, email: String)

    /** True when the failure means "no access / not found" rather than a transient problem. */
    fun isAccessProblem(error: Throwable): Boolean
}

class DriveDatabaseRemote(private val drive: DriveClient) : DatabaseRemote {
    override suspend fun create(name: String, content: String) = drive.create(name, content).toInfo()

    override suspend fun info(fileId: String): DatabaseRemote.FileInfo {
        val metadata = drive.metadata(fileId)
        if (metadata.trashed) throw DriveException(404, "File is in the trash")
        return metadata.toInfo()
    }

    override suspend fun download(fileId: String) = drive.download(fileId)

    override suspend fun update(fileId: String, content: String) = drive.update(fileId, content).toInfo()

    override suspend fun share(fileId: String, email: String) = drive.shareWith(fileId, email)

    override fun isAccessProblem(error: Throwable) = error is DriveException && error.isAccessProblem

    private fun com.neteinstein.networth.core.google.DriveFileMetadata.toInfo() = DatabaseRemote.FileInfo(id, name, version, webViewLink)
}
