package com.neteinstein.networth.core.google.oauth

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresInSeconds: Long = 3600,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    val scope: String? = null,
)

class OAuthException(message: String) : Exception(message)

/** Proof Key for Code Exchange (RFC 7636). */
object Pkce {
    /** 64 hex characters from a cryptographically secure source (UUID v4). */
    @OptIn(ExperimentalUuidApi::class)
    fun newVerifier(): String = Uuid.random().toHexString() + Uuid.random().toHexString()

    fun challenge(verifier: String): String =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(Sha256.digest(verifier.encodeToByteArray()))
}

/** Query parameters of a redirect such as `com.googleusercontent.apps.x:/oauth2redirect?code=…`. */
fun queryParameters(url: String): Map<String, String> = url.substringAfter('?', "").substringBefore('#')
    .split('&')
    .filter { it.isNotEmpty() }
    .associate { pair ->
        pair.substringBefore('=').decodeURLQueryComponent(plusIsSpace = true) to
            pair.substringAfter('=', "").decodeURLQueryComponent(plusIsSpace = true)
    }

/**
 * Google's OAuth 2.0 endpoints for an installed app (authorisation code + PKCE, no client secret),
 * used where there is no Google SDK (iOS).
 */
class GoogleOAuthClient(
    private val http: HttpClient,
    private val clientId: String,
    private val redirectUri: String,
) {
    fun authorizationUrl(scopes: List<String>, codeChallenge: String, extra: Map<String, String> = emptyMap()): String {
        val params = linkedMapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to scopes.joinToString(" "),
            "code_challenge" to codeChallenge,
            "code_challenge_method" to "S256",
        ) + extra
        return AUTH_URL + "?" + params.entries.joinToString("&") { (k, v) -> "$k=${v.encodeURLParameter()}" }
    }

    suspend fun exchangeCode(code: String, verifier: String): TokenResponse = token(
        "grant_type" to "authorization_code",
        "code" to code,
        "code_verifier" to verifier,
        "redirect_uri" to redirectUri,
    )

    suspend fun refresh(refreshToken: String): TokenResponse = token(
        "grant_type" to "refresh_token",
        "refresh_token" to refreshToken,
    )

    private suspend fun token(vararg fields: Pair<String, String>): TokenResponse {
        val response = http.submitForm(
            url = TOKEN_URL,
            formParameters = parameters {
                append("client_id", clientId)
                fields.forEach { (k, v) -> append(k, v) }
            },
        )
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw OAuthException("Token endpoint ${response.status.value}: ${body.take(300)}")
        return json.decodeFromString(body)
    }

    companion object {
        const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private val json = Json { ignoreUnknownKeys = true }

        /** iOS clients redirect to their reversed client id: `com.googleusercontent.apps.<id>`. */
        fun reversedClientId(clientId: String): String = clientId.split('.').reversed().joinToString(".")
    }
}
