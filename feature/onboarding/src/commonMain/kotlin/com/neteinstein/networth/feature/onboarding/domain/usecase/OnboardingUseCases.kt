package com.neteinstein.networth.feature.onboarding.domain.usecase

import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.DatabaseFile
import com.neteinstein.networth.core.domain.repository.DatabaseFolder
import com.neteinstein.networth.core.domain.repository.FamilyDatabaseRepository
import com.neteinstein.networth.core.domain.repository.NewDatabaseFile
import com.neteinstein.networth.core.domain.seed.HouseholdSetup

/** "Start fresh": a new database file in the signed-in user's Drive, named and placed by the app or by the user. */
class CreateHouseholdUseCase(private val database: FamilyDatabaseRepository) {
    suspend operator fun invoke(setup: HouseholdSetup, file: NewDatabaseFile = NewDatabaseFile()): DatabaseFile =
        database.createNew(setup, file)
}

/**
 * "Start fresh" → "Let me choose": shows the Google Picker for folders and returns the chosen one.
 * Returns null when the user closed the picker without choosing.
 */
class PickDatabaseFolderUseCase(
    private val auth: AuthRepository,
    private val database: FamilyDatabaseRepository,
) {
    suspend operator fun invoke(): DatabaseFolder? {
        val folderId = auth.pickDatabaseFolder() ?: return null
        return database.folder(folderId)
    }
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
