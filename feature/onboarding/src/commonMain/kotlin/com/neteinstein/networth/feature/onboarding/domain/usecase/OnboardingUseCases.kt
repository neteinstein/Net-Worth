package com.neteinstein.networth.feature.onboarding.domain.usecase

import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.DatabaseFile
import com.neteinstein.networth.core.domain.repository.FamilyDatabaseRepository
import com.neteinstein.networth.core.domain.seed.HouseholdSetup

/** "Start fresh": a new database file in the signed-in user's Drive. */
class CreateHouseholdUseCase(private val database: FamilyDatabaseRepository) {
    suspend operator fun invoke(setup: HouseholdSetup): DatabaseFile = database.createNew(setup)
}

/**
 * "Open a Google Drive file": shows the Google Picker and opens the chosen file.
 * Returns false when the user closed the picker without choosing.
 */
class OpenSharedDatabaseUseCase(
    private val auth: AuthRepository,
    private val database: FamilyDatabaseRepository,
) {
    suspend operator fun invoke(): Boolean {
        val fileId = auth.pickDatabaseFile() ?: return false
        database.open(fileId)
        return true
    }
}
