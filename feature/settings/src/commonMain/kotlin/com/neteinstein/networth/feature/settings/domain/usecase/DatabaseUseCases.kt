package com.neteinstein.networth.feature.settings.domain.usecase

import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.repository.FamilyDatabaseRepository
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase

/** Gives a family member (e.g. the other parent) edit access to the database file in Drive. */
class ShareDatabaseUseCase(private val database: FamilyDatabaseRepository) {
    suspend operator fun invoke(email: String) {
        require(EMAIL.matches(email.trim())) { "Not an email address" }
        database.share(email.trim())
    }

    companion object {
        private val EMAIL = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

        fun isEmail(text: String) = EMAIL.matches(text.trim())
    }
}

/** "Use a different database": forgets the current file on this device; it stays in Drive. */
class CloseDatabaseUseCase(private val database: FamilyDatabaseRepository) {
    suspend operator fun invoke() = database.close()
}

class RenameHouseholdUseCase(private val edit: EditDatabaseUseCase) {
    suspend operator fun invoke(name: String) {
        if (name.isBlank()) return
        edit { database, now -> database.copy(settings = database.settings.copy(householdName = name.trim(), updatedAt = now)) }
    }
}

class SaveMemberUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
) {
    /** Renames member [id], or adds a new one when [id] is null. */
    suspend operator fun invoke(id: String?, name: String, role: MemberRole) {
        if (name.isBlank()) return
        val memberId = id ?: ids.newId()
        edit { database, now -> database.copy(members = database.members.upsert(Member(memberId, name.trim(), role, now))) }
    }
}
