package com.neteinstein.networth.feature.settings.domain.usecase

import com.neteinstein.networth.core.domain.repository.ClaudeKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A hint of the saved Claude API key ("sk-ant-…x7Kq"), or null when there is none; the key itself never reaches the UI. */
class ObserveClaudeKeyHintUseCase(private val repository: ClaudeKeyRepository) {
    operator fun invoke(): Flow<String?> = repository.observeApiKey().map { key -> key?.let(::hint) }

    companion object {
        private const val SHOWN_START = 7
        private const val SHOWN_END = 4

        fun hint(key: String): String =
            if (key.length > SHOWN_START + SHOWN_END) "${key.take(SHOWN_START)}…${key.takeLast(SHOWN_END)}" else "••••"
    }
}

class SetClaudeKeyUseCase(private val repository: ClaudeKeyRepository) {
    /** Saves [key] on this device; null or blank removes it. */
    suspend operator fun invoke(key: String?) = repository.setApiKey(key)
}
