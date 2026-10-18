package com.neteinstein.networth.core.claude

import com.neteinstein.networth.core.domain.repository.InMemoryLocalStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalClaudeKeyRepositoryTest {
    @Test
    fun readsTheSavedKey() = runTest {
        val repository = LocalClaudeKeyRepository(InMemoryLocalStore(mapOf("claude-api-key.txt" to " sk-ant-1\n")))

        assertEquals("sk-ant-1", repository.observeApiKey().first())
    }

    @Test
    fun savesTrimmedAndRemovesBlank() = runTest {
        val store = InMemoryLocalStore()
        val repository = LocalClaudeKeyRepository(store)
        assertNull(repository.observeApiKey().first())

        repository.setApiKey("  sk-ant-2 ")
        assertEquals("sk-ant-2", repository.observeApiKey().first())
        assertEquals("sk-ant-2", store.read("claude-api-key.txt"))

        repository.setApiKey(" ")
        assertNull(repository.observeApiKey().first())
        assertNull(store.read("claude-api-key.txt"))
    }
}
