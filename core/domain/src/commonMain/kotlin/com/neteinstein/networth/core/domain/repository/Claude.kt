package com.neteinstein.networth.core.domain.repository

import com.neteinstein.networth.core.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * The family member's own Claude API key, used to file imported movements automatically. It is a
 * secret tied to one person's Anthropic account, so it stays on this device and is never written to
 * the shared family database.
 */
interface ClaudeKeyRepository {
    /** The saved key, or null when Claude isn't set up on this device. */
    fun observeApiKey(): Flow<String?>

    /** Saves [key]; null or blank removes it. */
    suspend fun setApiKey(key: String?)
}

/** One movement for Claude to file. [id] is the transaction's id. */
data class MovementToFile(
    val id: String,
    val description: String,
    val amountMinor: Long,
)

/** Where Claude would file a movement: one of the household's categories and, optionally, one of its subcategories. */
data class CategoryGuess(
    val categoryId: String,
    val subcategoryId: String?,
)

/** Why Claude couldn't file the movements. The UI maps each to a string. */
enum class CategorizerFailure {
    /** The key was rejected (401/403). */
    INVALID_KEY,

    /** Too many requests, or the account is out of credit. */
    RATE_LIMITED,

    /** No connection, a timeout, or Anthropic was unavailable. */
    NETWORK,

    /** Claude declined, or answered with something that couldn't be read. */
    UNKNOWN,
}

class CategorizerException(
    val failure: CategorizerFailure,
    cause: Throwable? = null,
) : Exception("Claude categorisation failed: $failure", cause)

/** Suggests categories for movements no rule or past choice could file. */
interface TransactionCategorizer {
    /**
     * Returns a guess per movement id, only for the movements Claude is confident about and only with
     * ids from [categories]. Throws [CategorizerException].
     */
    suspend fun categorize(
        apiKey: String,
        movements: List<MovementToFile>,
        categories: List<Category>,
        currencyCode: String,
    ): Map<String, CategoryGuess>
}
