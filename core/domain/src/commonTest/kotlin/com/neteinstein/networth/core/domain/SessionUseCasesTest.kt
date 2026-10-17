package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.core.domain.seed.HouseholdSetup
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.RestoreSessionUseCase
import com.neteinstein.networth.core.domain.usecase.SignInUseCase
import com.neteinstein.networth.core.domain.usecase.SignOutUseCase
import com.neteinstein.networth.core.domain.usecase.SyncDatabaseUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionUseCasesTest {
    private val auth = RecordingAuth()
    private val database = RecordingDatabase()

    @Test
    fun restoreOnlyOpensTheDatabaseWhenSignedIn() = runTest {
        RestoreSessionUseCase(auth, database)()
        assertEquals(emptyList(), database.calls)

        auth.savedUser = UserProfile("1", "a@b.c", null)
        RestoreSessionUseCase(auth, database)()
        assertEquals(listOf("restore"), database.calls)
    }

    @Test
    fun signInRestoresDatabaseAndSignOutClosesIt() = runTest {
        SignInUseCase(auth, database)()
        assertIs<AuthState.SignedIn>(ObserveAuthUseCase(auth)().value)
        assertEquals(DatabaseState.NotSelected, ObserveDatabaseUseCase(database)().value)

        SignOutUseCase(auth, database)()
        assertEquals(listOf("restore", "close"), database.calls)
        assertEquals(AuthState.SignedOut, auth.state.value)
    }

    @Test
    fun editAndSyncDelegate() = runTest {
        database.createNew(HouseholdSetup("F", listOf("A")))

        EditDatabaseUseCase(database).invoke { db, now -> db.copy(settings = db.settings.copy(householdName = "G", updatedAt = now)) }
        SyncDatabaseUseCase(database)()

        assertEquals("G", (database.state.value as DatabaseState.Ready).database.settings.householdName)
        assertEquals(listOf("create", "edit", "sync"), database.calls)
    }
}
