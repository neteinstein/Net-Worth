package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.database.schema.DatabaseDto
import com.neteinstein.networth.core.database.schema.FORMAT_ID
import com.neteinstein.networth.core.database.schema.SCHEMA_VERSION
import com.neteinstein.networth.core.database.schema.toDomain
import com.neteinstein.networth.core.database.schema.toDto
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.repository.DatabaseError
import com.neteinstein.networth.core.domain.repository.DatabaseException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Reads and writes the database file (see `schema/SchemaV1.kt` for the format). */
object FamilyDatabaseCodec {
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        // Files written by a newer app may carry fields this version doesn't know; keep reading them.
        ignoreUnknownKeys = true
    }

    fun encode(database: FamilyDatabase): String = json.encodeToString(DatabaseDto.serializer(), database.toDto())

    /** Throws [DatabaseException] with [DatabaseError.NOT_A_DATABASE] or [DatabaseError.NEWER_SCHEMA]. */
    fun decode(text: String): FamilyDatabase {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw DatabaseException(DatabaseError.NOT_A_DATABASE, e)
        }
        if (root["format"]?.jsonPrimitive?.content != FORMAT_ID) throw DatabaseException(DatabaseError.NOT_A_DATABASE)
        val version = runCatching { root.getValue("schemaVersion").jsonPrimitive.int }.getOrNull()
            ?: throw DatabaseException(DatabaseError.NOT_A_DATABASE)
        if (version > SCHEMA_VERSION) throw DatabaseException(DatabaseError.NEWER_SCHEMA)
        return try {
            json.decodeFromJsonElement(DatabaseDto.serializer(), migrate(root, version)).toDomain()
        } catch (e: Exception) {
            throw DatabaseException(DatabaseError.NOT_A_DATABASE, e)
        }
    }

    /** Upgrades older files step by step to [SCHEMA_VERSION]. Version 1 is the first, so nothing yet. */
    @Suppress("UNUSED_PARAMETER")
    private fun migrate(root: JsonObject, fromVersion: Int): JsonObject = root
}
