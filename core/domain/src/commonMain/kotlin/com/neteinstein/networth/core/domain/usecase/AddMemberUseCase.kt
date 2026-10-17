package com.neteinstein.networth.core.domain.usecase

import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.repository.IdGenerator

/**
 * Adds a person to the household from wherever an owner is picked (import, account editor), so
 * the new person can be selected straight away. Returns the new member.
 */
class AddMemberUseCase(
    private val edit: EditDatabaseUseCase,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(name: String, role: MemberRole): Member {
        require(name.isNotBlank()) { "Name is required" }
        val member = Member(ids.newId(), name.trim(), role)
        edit { database, now -> database.copy(members = database.members + member.copy(updatedAt = now)) }
        return member
    }
}
