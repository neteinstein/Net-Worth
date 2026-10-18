package com.neteinstein.networth.core.claude

import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.repository.CategorizerException
import com.neteinstein.networth.core.domain.repository.CategorizerFailure
import com.neteinstein.networth.core.domain.repository.CategoryGuess
import com.neteinstein.networth.core.domain.repository.MovementToFile
import com.neteinstein.networth.core.domain.repository.TransactionCategorizer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.abs

/**
 * Files movements with Claude through the Messages API (`POST /v1/messages`), called directly with the
 * family member's own key: there is no Net Worth server in between.
 *
 * Categories and movements are sent under short references (`c1`, `c1.2`, `7`) so the answer is small
 * and can only name things that exist; structured outputs constrain it to a JSON schema built from the
 * household's categories. Movements are sent in batches of [BATCH] to keep each answer quick.
 */
class ClaudeCategorizer(http: HttpClient) : TransactionCategorizer {
    private val http =
        http.config {
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT_MILLIS
                socketTimeoutMillis = TIMEOUT_MILLIS
            }
        }

    override suspend fun categorize(
        apiKey: String,
        movements: List<MovementToFile>,
        categories: List<Category>,
        currencyCode: String,
    ): Map<String, CategoryGuess> {
        if (movements.isEmpty() || categories.isEmpty()) return emptyMap()
        val refs = CategoryRefs(categories)
        return movements.chunked(BATCH).flatMap { batch -> request(apiKey, batch, refs, currencyCode).entries }
            .associate { it.key to it.value }
    }

    private suspend fun request(
        apiKey: String,
        batch: List<MovementToFile>,
        refs: CategoryRefs,
        currencyCode: String,
    ): Map<String, CategoryGuess> {
        val body = requestBody(batch, refs, currencyCode)
        val (status, text) =
            try {
                val response =
                    http.post(URL) {
                        header("x-api-key", apiKey)
                        header("anthropic-version", API_VERSION)
                        header("anthropic-beta", FALLBACK_BETA)
                        // The web build calls the API straight from the browser with the user's own key.
                        header("anthropic-dangerous-direct-browser-access", "true")
                        setBody(TextContent(body.toString(), ContentType.Application.Json))
                    }
                response.status.value to response.bodyAsText()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw CategorizerException(CategorizerFailure.NETWORK, e)
            }
        if (status !in 200..299) throw CategorizerException(failureFor(status, text))
        return parseAnswer(text, batch, refs)
    }

    internal fun requestBody(
        batch: List<MovementToFile>,
        refs: CategoryRefs,
        currencyCode: String,
    ): JsonObject = buildJsonObject {
        put("model", MODEL)
        put("max_tokens", MAX_TOKENS)
        // Server-side fallback: if a safety classifier declines, the API retries on a suitable model.
        put("fallbacks", "default")
        putJsonObject("output_config") {
            // Filing bank movements is classification: low effort keeps it quick and cheap.
            put("effort", "low")
            putJsonObject("format") {
                put("type", "json_schema")
                put("schema", schema(refs))
            }
        }
        put("system", SYSTEM_PROMPT)
        putJsonArray("messages") {
            addJsonObject {
                put("role", "user")
                put("content", prompt(batch, refs, currencyCode))
            }
        }
    }

    private fun prompt(
        batch: List<MovementToFile>,
        refs: CategoryRefs,
        currencyCode: String,
    ): String = buildString {
        appendLine("Categories (reference: name, then its subcategories):")
        refs.categories.forEach { (ref, category) ->
            append("- ").append(ref).append(": ").append(category.name)
            val subs = category.subcategories.mapIndexed { i, sub -> "$ref.${i + 1}: ${sub.name}" }
            if (subs.isNotEmpty()) append(" (").append(subs.joinToString("; ")).append(")")
            appendLine()
        }
        appendLine()
        appendLine("Bank movements (number | description | amount in $currencyCode, negative = money out):")
        batch.forEachIndexed { i, movement ->
            append(i + 1).append(" | ").append(movement.description.replace('\n', ' ')).append(" | ")
            appendLine(amount(movement.amountMinor))
        }
    }

    private fun schema(refs: CategoryRefs): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("movements") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("number") { put("type", "integer") }
                        putJsonObject("category") {
                            put("type", "string")
                            putJsonArray("enum") {
                                refs.categories.forEach { add(it.first) }
                                add("")
                            }
                        }
                        putJsonObject("subcategory") { put("type", "string") }
                    }
                    putJsonArray("required") {
                        add("number")
                        add("category")
                        add("subcategory")
                    }
                    put("additionalProperties", false)
                }
            }
        }
        putJsonArray("required") { add("movements") }
        put("additionalProperties", false)
    }

    internal fun parseAnswer(
        text: String,
        batch: List<MovementToFile>,
        refs: CategoryRefs,
    ): Map<String, CategoryGuess> {
        try {
            val message = JSON.parseToJsonElement(text).jsonObject
            if (message["stop_reason"]?.jsonPrimitive?.contentOrNull == "refusal") {
                throw CategorizerException(CategorizerFailure.UNKNOWN)
            }
            val answer =
                message["content"]?.jsonArray.orEmpty()
                    .map { it.jsonObject }
                    .lastOrNull { it["type"]?.jsonPrimitive?.contentOrNull == "text" }
                    ?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: throw CategorizerException(CategorizerFailure.UNKNOWN)
            val items = JSON.parseToJsonElement(answer).jsonObject["movements"] as? JsonArray ?: JsonArray(emptyList())
            return items.mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                val number = (item["number"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
                val movement = batch.getOrNull(number - 1) ?: return@mapNotNull null
                val category = (item["category"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                val subcategory = (item["subcategory"] as? JsonPrimitive)?.contentOrNull
                refs.resolve(category, subcategory)?.let { movement.id to it }
            }.toMap()
        } catch (e: CategorizerException) {
            throw e
        } catch (e: Exception) {
            throw CategorizerException(CategorizerFailure.UNKNOWN, e)
        }
    }

    /** Short, stable references for the household's categories (`c1`) and subcategories (`c1.2`). */
    internal class CategoryRefs(categories: List<Category>) {
        val categories: List<Pair<String, Category>> =
            categories.sortedBy { it.position }.mapIndexed { i, category -> "c${i + 1}" to category }
        private val byRef = this.categories.toMap()

        /** Null when Claude left the movement out ("") or named something that doesn't exist. */
        fun resolve(
            category: String,
            subcategory: String?,
        ): CategoryGuess? {
            val match = byRef[category] ?: return null
            val sub =
                subcategory?.takeIf { it.startsWith("$category.") }
                    ?.substringAfter('.')?.toIntOrNull()
                    ?.let { match.subcategories.getOrNull(it - 1) }
            return CategoryGuess(match.id, sub?.id)
        }
    }

    companion object {
        const val MODEL = "claude-opus-5-5"
        internal const val URL = "https://api.anthropic.com/v1/messages"
        internal const val API_VERSION = "2023-06-01"
        internal const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
        internal const val BATCH = 80
        private const val MAX_TOKENS = 16_000
        private const val TIMEOUT_MILLIS = 120_000L
        private val JSON = Json { ignoreUnknownKeys = true }

        private val SYSTEM_PROMPT =
            """
            You file a family's bank movements into the household's own spending categories.
            Descriptions come from Portuguese bank statements (e.g. "COMPRA 1234 CONTINENTE LISBOA",
            "MBWAY P/ JOAO", "DD EDP COMERCIAL"), so expect Portuguese merchants, abbreviations and
            payment prefixes.
            For each movement, give the reference of the category that fits it best and, when one of
            that category's subcategories clearly fits, the subcategory's reference; otherwise an empty
            subcategory. When you can't tell from the description alone (for example a payment to a
            person, or a generic card or ATM movement), give an empty category instead of guessing:
            the family will file those themselves. Answer every movement by its number.
            """.trimIndent()

        internal fun failureFor(
            status: Int,
            body: String,
        ): CategorizerFailure = when {
            status == 401 || status == 403 -> CategorizerFailure.INVALID_KEY
            status == 429 -> CategorizerFailure.RATE_LIMITED
            status == 400 && "credit balance" in body -> CategorizerFailure.RATE_LIMITED
            status >= 500 -> CategorizerFailure.NETWORK
            else -> CategorizerFailure.UNKNOWN
        }

        /** Minor units to a plain decimal ("-12.34"): unambiguous for the model, whatever the app's locale. */
        internal fun amount(minor: Long): String {
            val sign = if (minor < 0) "-" else ""
            val value = abs(minor)
            return "$sign${value / 100}.${(value % 100).toString().padStart(2, '0')}"
        }
    }
}
