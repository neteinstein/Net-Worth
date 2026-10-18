@file:OptIn(ExperimentalWasmJsInterop::class)

package com.neteinstein.networth.core.google

import com.neteinstein.networth.core.domain.repository.AuthCancelledException
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.core.google.config.GoogleConfig
import io.ktor.client.HttpClient
import kotlinx.coroutines.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.js.Promise
import kotlin.time.Clock

/**
 * Web: Google Identity Services token client for access tokens and the Google Picker for opening an
 * existing file or choosing the folder for a new one. Both scripts are loaded by `index.html`. GIS
 * has no refresh tokens, so a new token is requested (silently when the Google session allows it)
 * whenever the old one expires.
 */
internal class WebGoogleAuth(
    private val http: HttpClient,
    private val profiles: ProfileStore,
) : GoogleAuthPlatform {
    private val tokenLock = Mutex()
    private var token: String? = null
    private var tokenExpiresAt = 0L
    private var hint: String? = null

    override val isConfigured: Boolean get() = GoogleConfig.WEB_CLIENT_ID.isNotBlank()

    override suspend fun restore(): UserProfile? = profiles.load()?.also { hint = it.email }

    override suspend fun signIn(): UserProfile {
        val accessToken = request(SIGN_IN_SCOPES, prompt = "select_account")
        val profile = fetchGoogleUserInfo(http, accessToken)
        hint = profile.email
        profiles.save(profile)
        return profile
    }

    override suspend fun accessToken(forceRefresh: Boolean): String = tokenLock.withLock {
        val cached = token
        if (!forceRefresh && cached != null && now() < tokenExpiresAt) cached else request(SIGN_IN_SCOPES, prompt = "")
    }

    override suspend fun pickFile(): String? = pick(folders = false)

    override suspend fun pickFolder(): String? = pick(folders = true)

    private suspend fun pick(folders: Boolean): String? {
        val accessToken = accessToken(forceRefresh = false)
        val picked = try {
            jsPick(
                accessToken,
                GoogleConfig.API_KEY,
                GoogleConfig.PROJECT_NUMBER,
                DriveClient.JSON_MIME,
                folders,
            ).await<JsString?>()
        } catch (e: JsException) {
            // A JS rejection is not an Exception; rethrow as one so callers can report it.
            throw IllegalStateException(e.message, e)
        }
        return picked?.toString()?.takeIf { it.isNotEmpty() }
    }

    override suspend fun signOut() {
        token?.let { jsRevoke(it) }
        token = null
        hint = null
        profiles.clear()
    }

    private suspend fun request(scope: String, prompt: String): String {
        val response = try {
            jsRequestToken(GoogleConfig.WEB_CLIENT_ID, scope, prompt, hint ?: "").await<JsString>().toString()
        } catch (e: Throwable) {
            if (e.message?.contains("popup_closed") == true || e.message?.contains("access_denied") == true) throw AuthCancelledException()
            throw e
        }
        val parts = response.split('|')
        val accessToken = parts[0]
        val expiresIn = parts.getOrNull(1)?.toLongOrNull() ?: 3600L
        requireDriveScope(parts.getOrNull(2))
        token = accessToken
        tokenExpiresAt = now() + (expiresIn - 60) * 1000
        return accessToken
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val SIGN_IN_SCOPES = "openid email profile $DRIVE_FILE_SCOPE"
    }
}

/** Resolves to "accessToken|expiresInSeconds|grantedScopes". */
@JsFun(
    """(clientId, scope, prompt, hint) => new Promise((resolve, reject) => {
        if (!window.google || !google.accounts || !google.accounts.oauth2) { reject(new Error('Google Identity Services not loaded')); return; }
        const client = google.accounts.oauth2.initTokenClient({
            client_id: clientId,
            scope: scope,
            prompt: prompt,
            hint: hint || undefined,
            callback: (r) => r.error ? reject(new Error(r.error)) : resolve(r.access_token + '|' + r.expires_in + '|' + (r.scope || '')),
            error_callback: (e) => reject(new Error((e && e.type) || 'popup_closed')),
        });
        client.requestAccessToken();
    })""",
)
private external fun jsRequestToken(clientId: String, scope: String, prompt: String, hint: String): Promise<JsString>

/**
 * Resolves to the picked file (or, with [folders], folder) id, or null when the picker is closed;
 * rejects when the picker reports an error, so the caller is never left waiting.
 *
 * Folders: My Drive opens at its root, so top-level folders can be picked as well as the ones inside
 * them; a second tab lists shared drives.
 */
@JsFun(
    """(token, apiKey, appId, mimeTypes, folders) => new Promise((resolve, reject) => {
        if (!window.gapi) { reject(new Error('Google API loader not loaded')); return; }
        gapi.load('picker', () => {
            const folderView = () => new google.picker.DocsView(google.picker.ViewId.DOCS)
                .setIncludeFolders(true)
                .setSelectFolderEnabled(true)
                .setMimeTypes('application/vnd.google-apps.folder')
                .setMode(google.picker.DocsViewMode.LIST);
            const builder = new google.picker.PickerBuilder()
                .setOAuthToken(token)
                .setDeveloperKey(apiKey)
                .setAppId(appId)
                .setCallback((data) => {
                    const action = data[google.picker.Response.ACTION];
                    if (action === google.picker.Action.PICKED) {
                        const docs = data[google.picker.Response.DOCUMENTS] || [];
                        resolve(docs.length ? docs[0][google.picker.Document.ID] : null);
                    } else if (action === google.picker.Action.CANCEL) {
                        resolve(null);
                    } else if (action === google.picker.Action.ERROR) {
                        reject(new Error('Google Picker error'));
                    }
                });
            if (folders) {
                builder
                    .addView(folderView().setParent('root'))
                    .addView(folderView().setEnableDrives(true))
                    .enableFeature(google.picker.Feature.SUPPORT_DRIVES);
            } else {
                builder.addView(new google.picker.DocsView(google.picker.ViewId.DOCS).setMimeTypes(mimeTypes).setMode(google.picker.DocsViewMode.LIST));
            }
            builder.build().setVisible(true);
        });
    })""",
)
private external fun jsPick(token: String, apiKey: String, appId: String, mimeTypes: String, folders: Boolean): Promise<JsString?>

@JsFun("(token) => { if (window.google && google.accounts && google.accounts.oauth2) google.accounts.oauth2.revoke(token, () => {}); }")
private external fun jsRevoke(token: String)
