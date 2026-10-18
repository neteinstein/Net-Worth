package com.neteinstein.networth.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String? = null,
)

sealed interface AuthState {
    data object Unknown : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val user: UserProfile) : AuthState
}

/** The user closed the Google sign-in or picker without finishing. */
class AuthCancelledException : Exception("Cancelled by the user")

/** This build has no Google OAuth client configured (see docs/google-cloud-setup.md). */
class AuthNotConfiguredException : IllegalStateException("Google sign-in is not configured for this build")

/** The user signed in but left Google Drive unticked on Google's consent screen. */
class DriveAccessNotGrantedException : Exception("Google Drive access was not granted")

/** Google sign-in and Drive file picking. */
interface AuthRepository {
    val state: StateFlow<AuthState>

    /** Silently restores a previous session; ends in [AuthState.SignedIn] or [AuthState.SignedOut]. */
    suspend fun restore()

    /**
     * Interactive Google sign-in. Throws [AuthCancelledException] if the user backs out and
     * [DriveAccessNotGrantedException] if they sign in without allowing Drive access.
     */
    suspend fun signIn(): UserProfile

    /** Shows the Google Picker; returns the picked file id, or null if nothing was picked. */
    suspend fun pickDatabaseFile(): String?

    suspend fun signOut()
}
