package com.neteinstein.networth.core.claude

import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.Subcategory
import com.neteinstein.networth.core.domain.repository.CategorizerException
import com.neteinstein.networth.core.domain.repository.CategorizerFailure
import com.neteinstein.networth.core.domain.repository.CategoryGuess
import com.neteinstein.networth.core.domain.repository.MovementToFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClaudeCategorizerTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val categories =
        listOf(
            Category(
                "home",
                "Home",
                "#000000",
                listOf(Subcategory("rent", "Rent / mortgage"), Subcategory("power", "Utilities")),
                position = 1,
            ),
            Category("food", "Food", "#000000", listOf(Subcategory("groceries", "Groceries")), position = 0),
        )

    private val movements =
        listOf(
            MovementToFile("t1", "COMPRA 1234 CONTINENTE LISBOA", -4_250),
            MovementToFile("t2", "DD EDP COMERCIAL", -6_010),
            MovementToFile("t3", "MBWAY P/ JOAO", -1_000),
        )

    /** A Messages API response whose text block is [answer]. */
    private fun message(
        answer: String,
        stopReason: String = "end_turn",
    ): String = buildJsonObject {
        put("id", "msg_1")
        put("type", "message")
        put("stop_reason", stopReason)
        put(
            "content",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("type", "thinking")
                        put("thinking", "")
                    },
                )
                add(
                    buildJsonObject {
                        put("type", "text")
                        put("text", answer)
                    },
                )
            },
        )
    }.toString()

    private fun categorizer(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: (HttpRequestData) -> String,
    ): ClaudeCategorizer = ClaudeCategorizer(
        HttpClient(
            MockEngine { request ->
                requests += request
                respond(body(request), status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ) { expectSuccess = false },
    )

    @Test
    fun filesMovementsByTheirReferences() = runTest {
        // Sorted by position: c1 = Food, c2 = Home.
        val answer =
            """{"movements":[{"number":1,"category":"c1","subcategory":"c1.1"},{"number":2,"category":"c2","subcategory":"c2.2"},""" +
                """{"number":3,"category":"","subcategory":""}]}"""
        val claude = categorizer { message(answer) }

        val guesses = claude.categorize("sk-ant-test", movements, categories, "EUR")

        assertEquals(
            mapOf("t1" to CategoryGuess("food", "groceries"), "t2" to CategoryGuess("home", "power")),
            guesses,
        )
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://api.anthropic.com/v1/messages", request.url.toString())
        assertEquals("sk-ant-test", request.headers["x-api-key"])
        assertEquals("2023-06-01", request.headers["anthropic-version"])
        assertEquals("true", request.headers["anthropic-dangerous-direct-browser-access"])
        val sent = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals(ClaudeCategorizer.MODEL, sent["model"]!!.jsonPrimitive.content)
        val format = sent["output_config"]!!.jsonObject["format"]!!.jsonObject
        assertEquals("json_schema", format["type"]!!.jsonPrimitive.content)
        val prompt = sent["messages"]!!.jsonArray.single().jsonObject["content"]!!.jsonPrimitive.content
        assertTrue("- c1: Food (c1.1: Groceries)" in prompt, prompt)
        assertTrue("2 | DD EDP COMERCIAL | -60.10" in prompt, prompt)
    }

    @Test
    fun ignoresUnknownReferencesAndNumbers() = runTest {
        val answer =
            """{"movements":[{"number":1,"category":"c9","subcategory":""},{"number":7,"category":"c1","subcategory":""},""" +
                """{"number":2,"category":"c2","subcategory":"c1.1"}]}"""
        val claude = categorizer { message(answer) }

        val guesses = claude.categorize("key", movements, categories, "EUR")

        // A subcategory of another category is dropped, the category kept.
        assertEquals(mapOf("t2" to CategoryGuess("home", null)), guesses)
    }

    @Test
    fun sendsLargeImportsInBatches() = runTest {
        val many = (1..ClaudeCategorizer.BATCH + 5).map { MovementToFile("t$it", "SHOP $it", -100) }
        val claude = categorizer { message("""{"movements":[{"number":1,"category":"c1","subcategory":""}]}""") }

        val guesses = claude.categorize("key", many, categories, "EUR")

        assertEquals(2, requests.size)
        assertEquals(setOf("t1", "t${ClaudeCategorizer.BATCH + 1}"), guesses.keys)
    }

    @Test
    fun skipsTheCallWhenThereIsNothingToFile() = runTest {
        val claude = categorizer { error("no call expected") }

        assertEquals(emptyMap(), claude.categorize("key", emptyList(), categories, "EUR"))
        assertEquals(emptyMap(), claude.categorize("key", movements, emptyList(), "EUR"))
    }

    @Test
    fun mapsHttpErrors() = runTest {
        val cases =
            listOf(
                HttpStatusCode.Unauthorized to CategorizerFailure.INVALID_KEY,
                HttpStatusCode.TooManyRequests to CategorizerFailure.RATE_LIMITED,
                HttpStatusCode.InternalServerError to CategorizerFailure.NETWORK,
                HttpStatusCode.NotFound to CategorizerFailure.UNKNOWN,
            )
        for ((status, failure) in cases) {
            val error =
                assertFailsWith<CategorizerException> { categorizer(status) { "{}" }.categorize("key", movements, categories, "EUR") }
            assertEquals(failure, error.failure, status.toString())
        }
        val credit = """{"type":"error","error":{"type":"invalid_request_error","message":"Your credit balance is too low"}}"""
        val error = assertFailsWith<CategorizerException> {
            categorizer(HttpStatusCode.BadRequest) { credit }.categorize("key", movements, categories, "EUR")
        }
        assertEquals(CategorizerFailure.RATE_LIMITED, error.failure)
    }

    @Test
    fun refusalsAndUnreadableAnswersAreFailures() = runTest {
        val refused = assertFailsWith<CategorizerException> {
            categorizer { message("", stopReason = "refusal") }.categorize("key", movements, categories, "EUR")
        }
        assertEquals(CategorizerFailure.UNKNOWN, refused.failure)
        val garbled = assertFailsWith<CategorizerException> {
            categorizer { message("not json") }.categorize("key", movements, categories, "EUR")
        }
        assertEquals(CategorizerFailure.UNKNOWN, garbled.failure)
    }

    @Test
    fun connectionProblemsAreNetworkFailures() = runTest {
        val claude =
            ClaudeCategorizer(HttpClient(MockEngine { throw IllegalStateException("offline") }) { expectSuccess = false })

        val error = assertFailsWith<CategorizerException> { claude.categorize("key", movements, categories, "EUR") }

        assertEquals(CategorizerFailure.NETWORK, error.failure)
    }

    @Test
    fun formatsAmountsAsPlainDecimals() {
        assertEquals("-12.34", ClaudeCategorizer.amount(-1_234))
        assertEquals("0.05", ClaudeCategorizer.amount(5))
        assertEquals("1500.00", ClaudeCategorizer.amount(150_000))
    }
}
