package com.neteinstein.networth.core.domain.seed

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.HouseholdSettings
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.repository.IdGenerator

/** What "Start fresh" asks for. Account names are passed in so they can be localised by the UI. */
data class HouseholdSetup(
    val householdName: String,
    val adults: List<String>,
    val children: List<String> = emptyList(),
    val currencyCode: String = CurrencyCode.DEFAULT.code,
    val createAccounts: Boolean = true,
    val jointSavingsName: String = "Joint savings",
) {
    init {
        require(householdName.isNotBlank()) { "Household name is required" }
        require(adults.any { it.isNotBlank() }) { "At least one adult is required" }
    }
}

/**
 * Builds the initial database: members, one account per person (a current account per adult,
 * shared savings for the adults and a savings account per child, as in the design) and the
 * default categories.
 */
fun HouseholdSetup.toDatabase(ids: IdGenerator, now: Long, createdBy: String): FamilyDatabase {
    val adultMembers = adults.filter { it.isNotBlank() }.map { Member(ids.newId(), it.trim(), MemberRole.ADULT, now) }
    val childMembers = children.filter { it.isNotBlank() }.map { Member(ids.newId(), it.trim(), MemberRole.CHILD, now) }
    val accounts = if (!createAccounts) {
        emptyList()
    } else {
        buildList {
            adultMembers.forEach { add(Account(ids.newId(), it.name, AccountType.CURRENT, listOf(it.id), updatedAt = now)) }
            add(Account(ids.newId(), jointSavingsName, AccountType.SAVINGS, adultMembers.map { it.id }, updatedAt = now))
            childMembers.forEach { add(Account(ids.newId(), it.name, AccountType.SAVINGS, listOf(it.id), updatedAt = now)) }
        }
    }
    return FamilyDatabase(
        id = ids.newId(),
        createdAt = now,
        updatedAt = now,
        updatedBy = createdBy,
        settings = HouseholdSettings(householdName = householdName.trim(), currencyCode = currencyCode, updatedAt = now),
        members = adultMembers + childMembers,
        accounts = accounts,
        categories = DefaultCategories.create(ids, now),
    )
}
