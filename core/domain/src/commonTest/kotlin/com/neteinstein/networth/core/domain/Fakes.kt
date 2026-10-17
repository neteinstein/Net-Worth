package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseFile
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.FamilyDatabaseRepository
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.core.domain.seed.HouseholdSetup
import com.neteinstein.networth.core.domain.seed.toDatabase
import kotlinx.coroutines.flow.MutableStateFlow

class RecordingAuth(var savedUser: UserProfile? = null) : AuthRepository {
    val calls = mutableListOf<String>()
    override val state = MutableStateFlow<AuthState>(AuthState.Unknown)

    override suspend fun restore() {
        calls += "restore"
        state.value = savedUser?.let { AuthState.SignedIn(it) } ?: AuthState.SignedOut
    }

    override suspend fun signIn(): UserProfile {
        calls += "signIn"
        val user = UserProfile("1", "a@b.c", "Ana")
        state.value = AuthState.SignedIn(user)
        return user
    }

    override suspend fun pickDatabaseFile(): String? = "picked"

    override suspend fun signOut() {
        calls += "signOut"
        state.value = AuthState.SignedOut
    }
}

class RecordingDatabase : FamilyDatabaseRepository {
    val calls = mutableListOf<String>()
    override val state = MutableStateFlow<DatabaseState>(DatabaseState.Loading)

    override suspend fun restore() {
        calls += "restore"
        state.value = DatabaseState.NotSelected
    }

    override suspend fun createNew(setup: HouseholdSetup): DatabaseFile {
        calls += "create"
        val file = DatabaseFile("f", "n")
        state.value = DatabaseState.Ready(setup.toDatabase(SequentialIds(), 0, "a"), file, SyncStatus.SYNCED)
        return file
    }

    override suspend fun open(fileId: String) {
        calls += "open:$fileId"
    }

    override suspend fun edit(transform: (database: FamilyDatabase, now: Long) -> FamilyDatabase) {
        calls += "edit"
        val ready = state.value as? DatabaseState.Ready ?: return
        state.value = ready.copy(database = transform(ready.database, 7))
    }

    override suspend fun sync() {
        calls += "sync"
    }

    override suspend fun share(email: String) {
        calls += "share:$email"
    }

    override suspend fun close() {
        calls += "close"
        state.value = DatabaseState.NotSelected
    }
}
