package com.neteinstein.networth.core.google

import com.neteinstein.networth.core.domain.repository.AuthCancelledException
import com.neteinstein.networth.core.domain.repository.AuthNotConfiguredException
import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [AuthRepository] on top of the platform's Google sign-in; also hands Drive its access tokens. */
class GoogleAuthRepository(
    private val platform: GoogleAuthPlatform,
) : AuthRepository,
    AccessTokenProvider {
    private val mutableState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()

    override suspend fun restore() {
        val user = try {
            if (platform.isConfigured) platform.restore() else null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        mutableState.value = user?.let { AuthState.SignedIn(it) } ?: AuthState.SignedOut
    }

    override suspend fun signIn(): UserProfile {
        if (!platform.isConfigured) throw AuthNotConfiguredException()
        val user = platform.signIn()
        mutableState.value = AuthState.SignedIn(user)
        return user
    }

    override suspend fun pickDatabaseFile(): String? = pickOrNull { platform.pickFile() }

    override suspend fun pickDatabaseFolder(): String? = pickOrNull { platform.pickFolder() }

    private suspend fun pickOrNull(pick: suspend () -> String?): String? = try {
        pick()
    } catch (_: AuthCancelledException) {
        null
    }

    override suspend fun signOut() {
        try {
            platform.signOut()
        } finally {
            mutableState.value = AuthState.SignedOut
        }
    }

    override suspend fun token(forceRefresh: Boolean): String = platform.accessToken(forceRefresh)
}
