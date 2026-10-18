package com.neteinstein.networth.core.google

import com.neteinstein.networth.core.domain.repository.AuthCancelledException
import com.neteinstein.networth.core.domain.repository.AuthNotConfiguredException
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DriveAccessNotGrantedException
import com.neteinstein.networth.core.domain.repository.InMemoryLocalStore
import com.neteinstein.networth.core.domain.repository.UserProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FakeGoogleAuthPlatform(
    override val isConfigured: Boolean = true,
    var saved: UserProfile? = null,
) : GoogleAuthPlatform {
    val user = UserProfile("1", "ana@example.com", "Ana")
    var pick: () -> String? = { "file-1" }
    var failRestore = false

    override suspend fun restore(): UserProfile? = if (failRestore) error("boom") else saved

    override suspend fun signIn(): UserProfile = user.also { saved = it }

    override suspend fun accessToken(forceRefresh: Boolean): String = if (forceRefresh) "new" else "old"

    override suspend fun pickFile(): String? = pick()

    override suspend fun signOut() {
        saved = null
    }
}

class GoogleAuthRepositoryTest {
    @Test
    fun restoreReportsSignedInOrOut() = runTest {
        val platform = FakeGoogleAuthPlatform()
        val repo = GoogleAuthRepository(platform)
        assertEquals(AuthState.Unknown, repo.state.value)

        repo.restore()
        assertEquals(AuthState.SignedOut, repo.state.value)

        platform.saved = platform.user
        repo.restore()
        assertEquals(AuthState.SignedIn(platform.user), repo.state.value)

        platform.failRestore = true
        repo.restore()
        assertEquals(AuthState.SignedOut, repo.state.value)
    }

    @Test
    fun signInAndOut() = runTest {
        val platform = FakeGoogleAuthPlatform()
        val repo = GoogleAuthRepository(platform)

        assertEquals(platform.user, repo.signIn())
        assertEquals(AuthState.SignedIn(platform.user), repo.state.value)
        assertEquals("new", repo.token(forceRefresh = true))

        repo.signOut()
        assertEquals(AuthState.SignedOut, repo.state.value)
        assertNull(platform.saved)
    }

    @Test
    fun unconfiguredBuildRefusesSignIn() = runTest {
        val repo = GoogleAuthRepository(FakeGoogleAuthPlatform(isConfigured = false))

        assertFailsWith<AuthNotConfiguredException> { repo.signIn() }
        repo.restore()
        assertEquals(AuthState.SignedOut, repo.state.value)
    }

    @Test
    fun pickerCancellationMeansNoFile() = runTest {
        val platform = FakeGoogleAuthPlatform()
        val repo = GoogleAuthRepository(platform)
        assertEquals("file-1", repo.pickDatabaseFile())

        platform.pick = { throw AuthCancelledException() }
        assertNull(repo.pickDatabaseFile())
    }

    @Test
    fun profileStoreRoundTrips() = runTest {
        val store = ProfileStore(InMemoryLocalStore())
        assertNull(store.load())

        store.save(UserProfile("1", "a@b.c", null, "p"))
        assertEquals(UserProfile("1", "a@b.c", null, "p"), store.load())

        store.clear()
        assertNull(store.load())
        assertNull(ProfileStore(InMemoryLocalStore(mapOf("google-profile.json" to "garbage"))).load())
    }

    @Test
    fun signInWithoutDriveIsRefused() = runTest {
        // Drive unticked on the consent screen: the token has no drive.file scope.
        assertFailsWith<DriveAccessNotGrantedException> { requireDriveScope("openid email profile") }
        assertFailsWith<DriveAccessNotGrantedException> { requireDriveScope(listOf("openid")) }
        requireDriveScope("openid $DRIVE_FILE_SCOPE")
        requireDriveScope(listOf(DRIVE_FILE_SCOPE))
        // A response that doesn't list scopes is not a refusal.
        requireDriveScope(null as String?)

        val platform = object : GoogleAuthPlatform by FakeGoogleAuthPlatform() {
            override suspend fun signIn(): UserProfile = throw DriveAccessNotGrantedException()
        }
        val repo = GoogleAuthRepository(platform)
        assertFailsWith<DriveAccessNotGrantedException> { repo.signIn() }
        assertEquals(AuthState.Unknown, repo.state.value)
    }
}
