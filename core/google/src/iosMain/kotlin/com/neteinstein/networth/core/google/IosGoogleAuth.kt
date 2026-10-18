package com.neteinstein.networth.core.google

import com.neteinstein.networth.core.domain.repository.AuthCancelledException
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.core.google.config.GoogleConfig
import com.neteinstein.networth.core.google.oauth.GoogleOAuthClient
import com.neteinstein.networth.core.google.oauth.OAuthException
import com.neteinstein.networth.core.google.oauth.Pkce
import com.neteinstein.networth.core.google.oauth.TokenResponse
import com.neteinstein.networth.core.google.oauth.queryParameters
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorDomain
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock

/**
 * iOS: OAuth 2.0 authorisation code + PKCE in an ASWebAuthenticationSession, redirecting to the
 * iOS client's reversed client id. The refresh token lives in the Keychain. The Picker runs in the
 * same browser flow (`trigger_onepick=true`) and returns `picked_file_ids` on the redirect.
 */
internal class IosGoogleAuth(
    private val http: HttpClient,
    private val profiles: ProfileStore,
    private val keychain: KeychainStore,
) : GoogleAuthPlatform {
    private val scheme = GoogleOAuthClient.reversedClientId(GoogleConfig.IOS_CLIENT_ID)
    private val oauth = GoogleOAuthClient(http, GoogleConfig.IOS_CLIENT_ID, "$scheme:/oauth2redirect")
    private val tokenLock = Mutex()
    private var token: String? = null
    private var tokenExpiresAt = 0L

    override val isConfigured: Boolean get() = GoogleConfig.IOS_CLIENT_ID.isNotBlank()

    override suspend fun restore(): UserProfile? = if (keychain.get(REFRESH_TOKEN) != null) profiles.load() else null

    override suspend fun signIn(): UserProfile {
        val tokens = authorize(listOf("openid", "email", "profile", DRIVE_FILE_SCOPE), mapOf("prompt" to "consent")).second
        requireDriveScope(tokens.scope)
        val profile = fetchGoogleUserInfo(http, tokens.accessToken)
        profiles.save(profile)
        return profile
    }

    override suspend fun accessToken(forceRefresh: Boolean): String = tokenLock.withLock {
        val cached = token
        if (!forceRefresh && cached != null && now() < tokenExpiresAt) return cached
        val refreshToken = keychain.get(REFRESH_TOKEN) ?: throw OAuthException("Not signed in")
        remember(oauth.refresh(refreshToken)).accessToken
    }

    override suspend fun pickFile(): String? {
        val extra = mapOf(
            "prompt" to "consent",
            "access_type" to "offline",
            "trigger_onepick" to "true",
            "mimetypes" to DriveClient.JSON_MIME,
        )
        val (params, _) = authorize(listOf(DRIVE_FILE_SCOPE), extra, keepRefreshToken = false)
        return params["picked_file_ids"]?.split(',')?.firstOrNull { it.isNotBlank() }
    }

    override suspend fun signOut() {
        token = null
        keychain.set(REFRESH_TOKEN, null)
        profiles.clear()
    }

    /** Runs the browser flow and exchanges the code; returns the redirect's parameters and the tokens. */
    private suspend fun authorize(
        scopes: List<String>,
        extra: Map<String, String>,
        keepRefreshToken: Boolean = true,
    ): Pair<Map<String, String>, TokenResponse> {
        val verifier = Pkce.newVerifier()
        val callback = present(oauth.authorizationUrl(scopes, Pkce.challenge(verifier), extra))
        val params = queryParameters(callback)
        params["error"]?.let { if (it == "access_denied") throw AuthCancelledException() else throw OAuthException(it) }
        val code = params["code"] ?: throw OAuthException("No authorisation code in redirect")
        val tokens = oauth.exchangeCode(code, verifier)
        tokenLock.withLock {
            if (keepRefreshToken) remember(tokens) else rememberAccessOnly(tokens)
        }
        return params to tokens
    }

    private fun remember(tokens: TokenResponse): TokenResponse {
        tokens.refreshToken?.let { keychain.set(REFRESH_TOKEN, it) }
        return rememberAccessOnly(tokens)
    }

    private fun rememberAccessOnly(tokens: TokenResponse): TokenResponse {
        token = tokens.accessToken
        tokenExpiresAt = now() + (tokens.expiresInSeconds - 60) * 1000
        return tokens
    }

    private suspend fun present(url: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val session = ASWebAuthenticationSession(
                uRL = NSURL.URLWithString(url)!!,
                callbackURLScheme = scheme,
            ) { callbackUrl, error ->
                when {
                    callbackUrl != null -> continuation.resume(callbackUrl.absoluteString ?: "")
                    error != null &&
                        error.domain == ASWebAuthenticationSessionErrorDomain &&
                        error.code == ASWebAuthenticationSessionErrorCodeCanceledLogin -> continuation.resumeWithException(
                        AuthCancelledException(),
                    )
                    else -> continuation.resumeWithException(OAuthException(error?.localizedDescription ?: "Sign-in failed"))
                }
            }
            session.presentationContextProvider = anchorProvider
            session.prefersEphemeralWebBrowserSession = false
            continuation.invokeOnCancellation { session.cancel() }
            if (!session.start()) continuation.resumeWithException(OAuthException("Could not start the sign-in session"))
        }
    }

    private val anchorProvider = object : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
        override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor =
            UIApplication.sharedApplication.keyWindow ?: UIWindow()
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val REFRESH_TOKEN = "refresh_token"
    }
}
