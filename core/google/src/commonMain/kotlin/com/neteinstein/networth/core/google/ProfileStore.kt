package com.neteinstein.networth.core.google

import com.neteinstein.networth.core.domain.repository.LocalStore
import com.neteinstein.networth.core.domain.repository.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Remembers who signed in so the next launch can skip the sign-in screen. */
class ProfileStore(private val store: LocalStore) {
    @Serializable
    private data class StoredProfile(
        val id: String,
        val email: String,
        val displayName: String? = null,
        val photoUrl: String? = null,
    )

    suspend fun load(): UserProfile? = store.read(ENTRY)?.let { text ->
        runCatching { json.decodeFromString<StoredProfile>(text) }.getOrNull()
            ?.let { UserProfile(it.id, it.email, it.displayName, it.photoUrl) }
    }

    suspend fun save(profile: UserProfile) = store.write(
        ENTRY,
        json.encodeToString(StoredProfile(profile.id, profile.email, profile.displayName, profile.photoUrl)),
    )

    suspend fun clear() = store.delete(ENTRY)

    private companion object {
        const val ENTRY = "google-profile.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
