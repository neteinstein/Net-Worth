package com.neteinstein.networth.core.domain.usecase

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.FamilyDatabaseRepository
import com.neteinstein.networth.core.domain.repository.UserProfile
import kotlinx.coroutines.flow.StateFlow

/** Restores the Google session and, when signed in, the last opened database. Run once at start-up. */
class RestoreSessionUseCase(
    private val auth: AuthRepository,
    private val database: FamilyDatabaseRepository,
) {
    suspend operator fun invoke() {
        auth.restore()
        if (auth.state.value is AuthState.SignedIn) database.restore()
    }
}

class SignInUseCase(
    private val auth: AuthRepository,
    private val database: FamilyDatabaseRepository,
) {
    suspend operator fun invoke(): UserProfile = auth.signIn().also { database.restore() }
}

/** Signs out and forgets the database on this device (the Drive file is untouched). */
class SignOutUseCase(
    private val auth: AuthRepository,
    private val database: FamilyDatabaseRepository,
) {
    suspend operator fun invoke() {
        database.close()
        auth.signOut()
    }
}

class ObserveAuthUseCase(private val auth: AuthRepository) {
    operator fun invoke(): StateFlow<AuthState> = auth.state
}

class ObserveDatabaseUseCase(private val database: FamilyDatabaseRepository) {
    operator fun invoke(): StateFlow<DatabaseState> = database.state
}

/** Applies a change to the shared database; [transform] receives the current time in epoch millis. */
class EditDatabaseUseCase(private val database: FamilyDatabaseRepository) {
    suspend operator fun invoke(transform: (database: FamilyDatabase, now: Long) -> FamilyDatabase) = database.edit(transform)
}

class SyncDatabaseUseCase(private val database: FamilyDatabaseRepository) {
    suspend operator fun invoke() = database.sync()
}
