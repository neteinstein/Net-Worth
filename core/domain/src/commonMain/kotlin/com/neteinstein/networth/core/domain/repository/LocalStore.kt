package com.neteinstein.networth.core.domain.repository

/**
 * Small named text entries kept on this device only (the database cache, the signed-in profile).
 * Android/iOS use files in the app sandbox; the web uses `localStorage`.
 */
interface LocalStore {
    suspend fun read(name: String): String?

    suspend fun write(name: String, text: String)

    suspend fun delete(name: String)
}

/** In-memory [LocalStore], for tests and previews. */
class InMemoryLocalStore(initial: Map<String, String> = emptyMap()) : LocalStore {
    private val entries = initial.toMutableMap()

    override suspend fun read(name: String): String? = entries[name]

    override suspend fun write(name: String, text: String) {
        entries[name] = text
    }

    override suspend fun delete(name: String) {
        entries.remove(name)
    }
}
