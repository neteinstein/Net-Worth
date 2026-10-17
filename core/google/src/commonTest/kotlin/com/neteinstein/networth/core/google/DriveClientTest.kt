package com.neteinstein.networth.core.google

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DriveClientTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val tokenCalls = mutableListOf<Boolean>()

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): DriveClient {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return DriveClient(HttpClient(engine) { expectSuccess = false }) { force ->
            tokenCalls += force
            if (force) "fresh" else "stale"
        }
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val meta = """{"id":"f1","name":"Family.networth.json","version":"7","modifiedTime":"2026-10-01T10:00:00Z","extra":1}"""

    @Test
    fun createsFileWithMultipartUpload() = runTest {
        val drive = client { json(meta) }

        val created = drive.create("Family.networth.json", """{"a":1}""")

        assertEquals("f1", created.id)
        assertEquals("7", created.version)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.toString().startsWith("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"))
        assertEquals("Bearer stale", request.headers[HttpHeaders.Authorization])
        val body = (request.body as TextContent).text
        assertTrue(""""appProperties":{"netWorthDatabase":"1"}""" in body)
        assertTrue("""{"a":1}""" in body)
        assertEquals("multipart/related; boundary=networth-boundary", request.body.contentType.toString())
    }

    @Test
    fun readsMetadataDownloadsAndUpdates() = runTest {
        val drive = client { request ->
            when {
                "alt=media" in request.url.toString() -> json("""{"schemaVersion":1}""")
                else -> json(meta)
            }
        }

        assertEquals("Family.networth.json", drive.metadata("f1").name)
        assertEquals("""{"schemaVersion":1}""", drive.download("f1"))
        assertEquals("7", drive.update("f1", "{}").version)
        assertEquals(HttpMethod.Patch, requests.last().method)
        assertTrue("upload/drive/v3/files/f1?uploadType=media" in requests.last().url.toString())
    }

    @Test
    fun retriesOnceWithFreshTokenAfter401() = runTest {
        val drive = client { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer stale") json("{}", HttpStatusCode.Unauthorized) else json(meta)
        }

        drive.metadata("f1")

        assertEquals(listOf(false, true), tokenCalls)
        assertEquals("Bearer fresh", requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun userInfoErrorsThrow() = runTest {
        val http = HttpClient(MockEngine { json("{}", HttpStatusCode.Unauthorized) })

        assertFailsWith<DriveException> { fetchGoogleUserInfo(http, "bad") }
    }

    @Test
    fun surfacesErrors() = runTest {
        val drive = client { json("""{"error":"notFound"}""", HttpStatusCode.NotFound) }

        val error = assertFailsWith<DriveException> { drive.download("missing") }

        assertEquals(404, error.status)
        assertTrue(error.isAccessProblem)
        assertFalse(DriveException(500, "x").isAccessProblem)
    }

    @Test
    fun sharesAndReadsUserInfo() = runTest {
        val drive = client { json("{}") }

        drive.shareWith("f1", "partner@example.com")
        val user = fetchGoogleUserInfo(HttpClient(MockEngine { json("""{"sub":"42","email":"a@b.c","name":"Ana","picture":"p"}""") }), "t")

        assertTrue(""""emailAddress":"partner@example.com"""" in (requests.first().body as TextContent).text)
        assertTrue(""""role":"writer"""" in (requests.first().body as TextContent).text)
        assertEquals("Ana", user.displayName)
        assertEquals("42", user.id)
    }
}
