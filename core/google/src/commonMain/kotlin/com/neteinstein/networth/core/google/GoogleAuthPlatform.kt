package com.neteinstein.networth.core.google

import com.neteinstein.networth.core.domain.repository.DriveAccessNotGrantedException
import com.neteinstein.networth.core.domain.repository.UserProfile

/** The only scope the app asks for: files the app created or the user picked. Non-sensitive. */
const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

/**
 * Google's consent screen lets the user untick Drive and still sign in. [grantedScopes] is what
 * the token response says was granted; null when it doesn't say, which isn't treated as a refusal.
 */
fun requireDriveScope(grantedScopes: Collection<String>?) {
    if (grantedScopes != null && DRIVE_FILE_SCOPE !in grantedScopes) throw DriveAccessNotGrantedException()
}

/** [requireDriveScope] for a space-separated `scope` field, as OAuth token responses carry it. */
fun requireDriveScope(scopeField: String?) = requireDriveScope(scopeField?.split(' ')?.filter(String::isNotBlank))

/** Per-platform Google sign-in, Drive authorisation and Picker. */
interface GoogleAuthPlatform {
    val isConfigured: Boolean

    /** The previous session's user, without any UI, or null. */
    suspend fun restore(): UserProfile?

    /** Interactive sign-in that also grants [DRIVE_FILE_SCOPE]; see [requireDriveScope]. */
    suspend fun signIn(): UserProfile

    /** A valid access token for Drive; [forceRefresh] after the API rejected the last one. */
    suspend fun accessToken(forceRefresh: Boolean): String

    /** Google Picker; returns the picked file id, or null. Grants the app access to that file. */
    suspend fun pickFile(): String?

    suspend fun signOut()
}
