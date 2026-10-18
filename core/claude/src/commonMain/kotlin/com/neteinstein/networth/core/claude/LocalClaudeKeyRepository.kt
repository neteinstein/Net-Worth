package com.neteinstein.networth.core.claude

import com.neteinstein.networth.core.domain.repository.ClaudeKeyRepository
import com.neteinstein.networth.core.domain.repository.LocalStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps the Claude API key in this device's [LocalStore] (app sandbox files, or `localStorage` on the web). */
class LocalClaudeKeyRepository(private val store: LocalStore) : ClaudeKeyRepository {
    private val key = MutableStateFlow<String?>(null)
    private var loaded = false
    private val lock = Mutex()

    override fun observeApiKey(): Flow<String?> = flow {
        load()
        emitAll(key)
    }

    override suspend fun setApiKey(key: String?) {
        val trimmed = key?.trim()?.takeIf { it.isNotEmpty() }
        lock.withLock {
            loaded = true
            this.key.value = trimmed
            if (trimmed == null) store.delete(ENTRY) else store.write(ENTRY, trimmed)
        }
    }

    private suspend fun load() = lock.withLock {
        if (!loaded) {
            key.value = store.read(ENTRY)?.trim()?.takeIf { it.isNotEmpty() }
            loaded = true
        }
    }

    private companion object {
        const val ENTRY = "claude-api-key.txt"
    }
}
