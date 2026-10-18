package com.neteinstein.networth.core.testing

import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.repository.CategorizerException
import com.neteinstein.networth.core.domain.repository.CategorizerFailure
import com.neteinstein.networth.core.domain.repository.CategoryGuess
import com.neteinstein.networth.core.domain.repository.ClaudeKeyRepository
import com.neteinstein.networth.core.domain.repository.MovementToFile
import com.neteinstein.networth.core.domain.repository.TransactionCategorizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeClaudeKeyRepository(key: String? = null) : ClaudeKeyRepository {
    val key = MutableStateFlow(key)

    override fun observeApiKey(): Flow<String?> = key

    override suspend fun setApiKey(key: String?) {
        this.key.value = key?.trim()?.takeIf { it.isNotEmpty() }
    }
}

/** Files every movement whose description contains a key of [byText]; fails with [failure] when set. */
class FakeCategorizer(
    private val byText: Map<String, CategoryGuess> = emptyMap(),
    var failure: CategorizerFailure? = null,
) : TransactionCategorizer {
    /** The movements of each call, in order. */
    val calls = mutableListOf<List<MovementToFile>>()

    override suspend fun categorize(
        apiKey: String,
        movements: List<MovementToFile>,
        categories: List<Category>,
        currencyCode: String,
    ): Map<String, CategoryGuess> {
        calls += movements
        failure?.let { throw CategorizerException(it) }
        return movements.mapNotNull { movement ->
            byText.entries.firstOrNull { movement.description.contains(it.key, ignoreCase = true) }?.let { movement.id to it.value }
        }.toMap()
    }
}
