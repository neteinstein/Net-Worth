package com.neteinstein.networth.core.google

import com.neteinstein.networth.core.google.oauth.GoogleOAuthClient
import com.neteinstein.networth.core.google.oauth.OAuthException
import com.neteinstein.networth.core.google.oauth.Pkce
import com.neteinstein.networth.core.google.oauth.Sha256
import com.neteinstein.networth.core.google.oauth.queryParameters
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OAuthTest {
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    @Test
    fun sha256MatchesKnownVectors() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex(Sha256.digest(ByteArray(0))))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex(Sha256.digest("abc".encodeToByteArray())))
        val long = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
        assertEquals("248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1", hex(Sha256.digest(long.encodeToByteArray())))
    }

    @Test
    fun pkceChallengeFollowsRfc7636Example() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
        val verifier = Pkce.newVerifier()
        assertEquals(64, verifier.length)
        assertTrue(verifier != Pkce.newVerifier())
    }

    @Test
    fun buildsAuthorizationUrlAndParsesRedirect() {
        val client = GoogleOAuthClient(HttpClient(MockEngine { respond("") }), "123-abc.apps.googleusercontent.com", "app:/cb")

        val url = client.authorizationUrl(listOf("openid", DRIVE_FILE_SCOPE), "chal", mapOf("trigger_onepick" to "true"))

        assertTrue(url.startsWith(GoogleOAuthClient.AUTH_URL + "?client_id=123-abc.apps.googleusercontent.com"))
        assertTrue("scope=openid%20https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fdrive.file" in url)
        assertTrue("code_challenge_method=S256" in url && url.endsWith("trigger_onepick=true"))
        assertEquals(
            mapOf("code" to "4/0A b", "picked_file_ids" to "f1,f2"),
            queryParameters("app:/cb?code=4%2F0A+b&picked_file_ids=f1%2Cf2#x"),
        )
        assertEquals(emptyMap(), queryParameters("app:/cb"))
        assertEquals("com.googleusercontent.apps.123-abc", GoogleOAuthClient.reversedClientId("123-abc.apps.googleusercontent.com"))
    }

    @Test
    fun exchangesAndRefreshesTokens() = runTest {
        val forms = mutableListOf<String>()
        val engine = MockEngine { request ->
            forms += (request.body as FormDataContent).formData.entries().joinToString("&") { "${it.key}=${it.value.single()}" }
            respond(
                """{"access_token":"at","expires_in":3599,"refresh_token":"rt","scope":"x","token_type":"Bearer"}""",
                HttpStatusCode.OK,
                headersOf("Content-Type", "application/json"),
            )
        }
        val client = GoogleOAuthClient(HttpClient(engine), "cid", "app:/cb")

        val tokens = client.exchangeCode("code1", "ver1")
        client.refresh("rt")

        assertEquals("at", tokens.accessToken)
        assertEquals("rt", tokens.refreshToken)
        assertEquals(3599, tokens.expiresInSeconds)
        assertEquals("client_id=cid&grant_type=authorization_code&code=code1&code_verifier=ver1&redirect_uri=app:/cb", forms[0])
        assertEquals("client_id=cid&grant_type=refresh_token&refresh_token=rt", forms[1])
    }

    @Test
    fun tokenErrorsThrow() = runTest {
        val client =
            GoogleOAuthClient(HttpClient(MockEngine { respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest) }), "c", "r")

        assertFailsWith<OAuthException> { client.refresh("old") }
    }
}
