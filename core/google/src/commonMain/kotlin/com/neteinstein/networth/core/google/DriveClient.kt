package com.neteinstein.networth.core.google

import com.neteinstein.networth.core.domain.repository.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun interface AccessTokenProvider {
    suspend fun token(forceRefresh: Boolean): String
}

@Serializable
data class DriveFileMetadata(
    val id: String,
    val name: String,
    /** Monotonically increasing; changes whenever the content changes. */
    val version: String? = null,
    val modifiedTime: String? = null,
    val webViewLink: String? = null,
    val trashed: Boolean = false,
)

/** A Drive API error. 401/403/404 mean the file can't be reached with the current grant. */
class DriveException(
    val status: Int,
    message: String,
) : Exception("Drive API $status: $message") {
    val isAccessProblem: Boolean get() = status == 401 || status == 403 || status == 404
}

/**
 * The few Drive v3 REST calls the app needs. Every call carries a bearer token; a 401 refreshes the
 * token once and retries.
 */
class DriveClient(
    private val http: HttpClient,
    private val tokens: AccessTokenProvider,
) {
    suspend fun create(name: String, content: String): DriveFileMetadata {
        val metadata = buildJsonObject {
            put("name", name)
            put("mimeType", JSON_MIME)
            putJsonObject("appProperties") { put(APP_PROPERTY, "1") }
        }
        val body = buildString {
            append("--$BOUNDARY\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata.toString())
            append("\r\n--$BOUNDARY\r\nContent-Type: $JSON_MIME\r\n\r\n")
            append(content)
            append("\r\n--$BOUNDARY--")
        }
        val response = call(HttpMethod.Post, "$UPLOAD/files?uploadType=multipart&fields=$FIELDS") {
            setBody(TextContent(body, ContentType.parse("multipart/related; boundary=$BOUNDARY")))
        }
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun metadata(fileId: String): DriveFileMetadata =
        json.decodeFromString(call(HttpMethod.Get, "$API/files/$fileId?fields=$FIELDS&supportsAllDrives=true").bodyAsText())

    suspend fun download(fileId: String): String = call(HttpMethod.Get, "$API/files/$fileId?alt=media&supportsAllDrives=true").bodyAsText()

    suspend fun update(fileId: String, content: String): DriveFileMetadata {
        val response = call(HttpMethod.Patch, "$UPLOAD/files/$fileId?uploadType=media&fields=$FIELDS&supportsAllDrives=true") {
            setBody(TextContent(content, ContentType.Application.Json))
        }
        return json.decodeFromString(response.bodyAsText())
    }

    /** Gives [email] edit access and emails them a link. */
    suspend fun shareWith(fileId: String, email: String) {
        val permission = buildJsonObject {
            put("type", "user")
            put("role", "writer")
            put("emailAddress", email)
        }
        call(HttpMethod.Post, "$API/files/$fileId/permissions?sendNotificationEmail=true&supportsAllDrives=true") {
            setBody(TextContent(permission.toString(), ContentType.Application.Json))
        }
    }

    private suspend fun call(method: HttpMethod, url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        var response = send(method, url, tokens.token(forceRefresh = false), block)
        if (response.status == HttpStatusCode.Unauthorized) {
            response = send(method, url, tokens.token(forceRefresh = true), block)
        }
        if (!response.status.isSuccess()) throw DriveException(response.status.value, response.bodyAsText().take(500))
        return response
    }

    private suspend fun send(method: HttpMethod, url: String, token: String, block: HttpRequestBuilder.() -> Unit): HttpResponse =
        http.request(url) {
            this.method = method
            bearerAuth(token)
            block()
        }

    companion object {
        const val JSON_MIME = "application/json"

        /** Marks files the app created, so they can be told apart from other JSON. */
        const val APP_PROPERTY = "netWorthDatabase"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val FIELDS = "id,name,version,modifiedTime,webViewLink,trashed"
        private const val BOUNDARY = "networth-boundary"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class UserInfo(
    val sub: String,
    val email: String,
    val name: String? = null,
    val picture: String? = null,
)

private val userInfoJson = Json { ignoreUnknownKeys = true }

/** The signed-in user's profile from an access token that includes the openid/email/profile scopes. */
suspend fun fetchGoogleUserInfo(http: HttpClient, accessToken: String): UserProfile {
    val response = http.request("https://openidconnect.googleapis.com/v1/userinfo") { bearerAuth(accessToken) }
    if (!response.status.isSuccess()) throw DriveException(response.status.value, response.bodyAsText().take(300))
    val info = userInfoJson.decodeFromString<UserInfo>(response.bodyAsText())
    return UserProfile(info.sub, info.email, info.name, info.picture)
}
