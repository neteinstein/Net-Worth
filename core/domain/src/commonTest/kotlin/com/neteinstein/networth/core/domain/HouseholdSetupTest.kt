package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.model.withTombstone
import com.neteinstein.networth.core.domain.seed.DefaultCategories
import com.neteinstein.networth.core.domain.seed.HouseholdSetup
import com.neteinstein.networth.core.domain.seed.toDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HouseholdSetupTest {
    @Test
    fun createsMembersAccountsAndDefaultCategories() {
        val setup = HouseholdSetup("Vicente", adults = listOf("Pedro", "Maria", " "), children = listOf("Inês"))

        val db = setup.toDatabase(SequentialIds(), now = 42, createdBy = "pedro@example.com")

        assertEquals(listOf("Pedro", "Maria", "Inês"), db.members.map { it.name })
        assertEquals(listOf(MemberRole.ADULT, MemberRole.ADULT, MemberRole.CHILD), db.members.map { it.role })
        assertEquals(listOf("Pedro", "Maria", "Joint savings", "Inês"), db.accounts.map { it.name })
        assertEquals(
            listOf(AccountType.CURRENT, AccountType.CURRENT, AccountType.SAVINGS, AccountType.SAVINGS),
            db.accounts.map {
                it.type
            },
        )
        assertEquals(db.members.take(2).map { it.id }, db.accounts[2].ownerIds)
        assertEquals(11, db.categories.size)
        assertEquals("Housing", db.categories.first().name)
        assertEquals(7, db.categories.first().subcategories.size)
        assertEquals(41, db.categories.sumOf { it.subcategories.size })
        assertEquals(42, db.updatedAt)
        assertEquals("pedro@example.com", db.updatedBy)
        assertEquals("EUR", db.currency.code)
        assertEquals(6, DefaultCategories.palette.size)
    }

    @Test
    fun canSkipAccounts() {
        val db = HouseholdSetup("Solo", adults = listOf("Ana"), createAccounts = false).toDatabase(SequentialIds(), 0, "a")

        assertTrue(db.accounts.isEmpty())
    }

    @Test
    fun requiresNameAndAnAdult() {
        assertFailsWith<IllegalArgumentException> { HouseholdSetup(" ", adults = listOf("A")) }
        assertFailsWith<IllegalArgumentException> { HouseholdSetup("X", adults = listOf("")) }
    }

    @Test
    fun upsertReplacesOrAppendsAndTombstonesRecordDeletes() {
        val db = TestFamily.database()
        val renamed = db.accounts.upsert(TestFamily.joint.copy(name = "Family savings"))
        val appended = db.accounts.upsert(TestFamily.joint.copy(id = "new"))

        assertEquals("Family savings", renamed.first { it.id == TestFamily.joint.id }.name)
        assertEquals(db.accounts.size, renamed.size)
        assertEquals(db.accounts.size + 1, appended.size)
        assertEquals(mapOf("x" to 7L), db.withTombstone("x", 7).tombstones)
        assertEquals(TestFamily.joint, db.account(TestFamily.joint.id))
        assertEquals(TestFamily.school, db.category(TestFamily.school.id))
        assertEquals(TestFamily.child, db.member(TestFamily.child.id))
    }
}
