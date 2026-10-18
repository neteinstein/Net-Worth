package com.neteinstein.networth.core.google

import android.accounts.Account
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.neteinstein.networth.core.domain.repository.AuthCancelledException
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.core.google.config.GoogleConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android: Credential Manager ("Sign in with Google") for identity, then the Identity
 * AuthorizationClient for the drive.file access token. The Picker runs inside the authorisation
 * flow (PICKER_OAUTH_TRIGGER) and returns the picked ids in `picked_file_ids`.
 */
internal class AndroidGoogleAuth(
    context: Context,
    private val profiles: ProfileStore,
) : GoogleAuthPlatform {
    private val app = context.applicationContext as Application
    private var foreground: WeakReference<ComponentActivity>? = null
    private val tokenLock = Mutex()
    private var user: UserProfile? = null
    private var token: String? = null
    private var tokenExpiresAt = 0L

    init {
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = track(activity)

                override fun onActivityStarted(activity: Activity) = track(activity)

                override fun onActivityResumed(activity: Activity) = track(activity)

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) {
                    if (foreground?.get() === activity) foreground = null
                }
            },
        )
    }

    private fun track(activity: Activity) {
        if (activity is ComponentActivity) foreground = WeakReference(activity)
    }

    private fun activity(): ComponentActivity = foreground?.get() ?: error("No foreground activity for Google sign-in")

    override val isConfigured: Boolean get() = GoogleConfig.WEB_CLIENT_ID.isNotBlank()

    override suspend fun restore(): UserProfile? = profiles.load().also { user = it }

    override suspend fun signIn(): UserProfile = withContext(Dispatchers.Main) {
        val activity = activity()
        val option = GetSignInWithGoogleOption.Builder(GoogleConfig.WEB_CLIENT_ID).build()
        val response = try {
            CredentialManager.create(activity).getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
        } catch (_: GetCredentialCancellationException) {
            throw AuthCancelledException()
        }
        val credential = response.credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type ${credential.type}"
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        val profile = UserProfile(
            id = google.id,
            email = google.id,
            displayName = google.displayName,
            photoUrl = google.profilePictureUri?.toString(),
        )
        user = profile
        // Ask for Drive access now, while the user is in the sign-in flow.
        accessToken(forceRefresh = true)
        profiles.save(profile)
        profile
    }

    override suspend fun accessToken(forceRefresh: Boolean): String = tokenLock.withLock {
        val cached = token
        if (!forceRefresh && cached != null && System.currentTimeMillis() < tokenExpiresAt) return cached
        val client = Identity.getAuthorizationClient(app)
        if (forceRefresh && cached != null) {
            runCatching { client.clearToken(ClearTokenRequest.builder().setToken(cached).build()).await() }
        }
        val result = authorize(driveRequest().build())
        requireDriveScope(result.grantedScopes)
        val fresh = result.accessToken ?: error("Drive authorisation returned no access token")
        token = fresh
        // Google access tokens last an hour; refresh a little early.
        tokenExpiresAt = System.currentTimeMillis() + TOKEN_LIFETIME_MS
        fresh
    }

    override suspend fun pickFile(): String? {
        val request = driveRequest()
            .setOptOutIncludingGrantedScopes(true)
            .setPrompt(AuthorizationRequest.Prompt.CONSENT)
            .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_OAUTH_TRIGGER, "true")
            .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_MIMETYPES, DriveClient.JSON_MIME)
            .build()
        val result = authorize(request)
        result.accessToken?.let {
            tokenLock.withLock {
                token = it
                tokenExpiresAt = System.currentTimeMillis() + TOKEN_LIFETIME_MS
            }
        }
        return result.tokenResponseParams?.getString(PICKED_FILE_IDS)?.split(',')?.firstOrNull { it.isNotBlank() }
    }

    override suspend fun signOut() {
        token = null
        user = null
        profiles.clear()
        runCatching { CredentialManager.create(app).clearCredentialState(ClearCredentialStateRequest()) }
    }

    private fun driveRequest(): AuthorizationRequest.Builder {
        val builder = AuthorizationRequest.Builder().setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
        user?.email?.let { builder.setAccount(Account(it, GOOGLE_ACCOUNT_TYPE)) }
        return builder
    }

    /** Runs an authorisation request, showing Google's consent/picker UI when it needs the user. */
    private suspend fun authorize(request: AuthorizationRequest): AuthorizationResult {
        val client = Identity.getAuthorizationClient(app)
        val result = client.authorize(request).await()
        if (!result.hasResolution()) return result
        val sender = result.pendingIntent?.intentSender ?: error("Authorisation needs UI but has no intent")
        val outcome = withContext(Dispatchers.Main) { activity().launchForResult(sender) }
        if (outcome.resultCode != Activity.RESULT_OK || outcome.data == null) throw AuthCancelledException()
        return client.getAuthorizationResultFromIntent(outcome.data)
    }

    private suspend fun ComponentActivity.launchForResult(sender: IntentSender): ActivityResult =
        suspendCancellableCoroutine { continuation ->
            val key = "networth-google-auth-${requestCounter.incrementAndGet()}"
            var launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>? = null
            launcher = activityResultRegistry.register(key, ActivityResultContracts.StartIntentSenderForResult()) { result ->
                launcher?.unregister()
                if (continuation.isActive) continuation.resume(result)
            }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(IntentSenderRequest.Builder(sender).build())
        }

    private companion object {
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
        const val PICKED_FILE_IDS = "picked_file_ids"
        const val TOKEN_LIFETIME_MS = 50 * 60 * 1000L
        val requestCounter = AtomicInteger()
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
