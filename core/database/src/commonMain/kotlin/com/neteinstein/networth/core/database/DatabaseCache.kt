package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.repository.LocalStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** This device's copy of the open database plus what it knows about the Drive file. */
internal data class CachedDatabase(
    val fileId: String,
    val fileName: String,
    val webViewLink: String?,
    /** Drive `version` of the file content this copy was last synced with. */
    val remoteVersion: String?,
    /** True while local edits have not been uploaded. */
    val pending: Boolean,
    val database: FamilyDatabase,
)

internal class DatabaseCache(private val store: LocalStore) {
    @Serializable
    private data class Entry(
        val fileId: String,
        val fileName: String,
        val webViewLink: String? = null,
        val remoteVersion: String? = null,
        val pending: Boolean = false,
        val database: String,
    )

    suspend fun load(): CachedDatabase? {
        val text = store.read(ENTRY) ?: return null
        return runCatching {
            val entry = json.decodeFromString<Entry>(text)
            CachedDatabase(
                entry.fileId,
                entry.fileName,
                entry.webViewLink,
                entry.remoteVersion,
                entry.pending,
                FamilyDatabaseCodec.decode(entry.database),
            )
        }.getOrNull()
    }

    suspend fun save(cached: CachedDatabase) = store.write(
        ENTRY,
        json.encodeToString(
            Entry(
                cached.fileId,
                cached.fileName,
                cached.webViewLink,
                cached.remoteVersion,
                cached.pending,
                FamilyDatabaseCodec.encode(cached.database),
            ),
        ),
    )

    suspend fun clear() = store.delete(ENTRY)

    private companion object {
        const val ENTRY = "database-cache.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
