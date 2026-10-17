package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.HouseholdSettings
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.repository.IdGenerator
import kotlinx.datetime.LocalDate

/** Sequential ids ("id-1", "id-2", …) for deterministic assertions. */
class SequentialIds : IdGenerator {
    private var next = 1

    override fun newId(): String = "id-${next++}"
}

/** A small family: two adults with current accounts, joint savings, one child's savings. */
object TestFamily {
    val adultA = Member("m-a", "Ana", MemberRole.ADULT)
    val adultB = Member("m-b", "Bruno", MemberRole.ADULT)
    val child = Member("m-c", "Carla", MemberRole.CHILD)
    val currentA = Account("acc-a", "Ana", AccountType.CURRENT, listOf(adultA.id))
    val currentB = Account("acc-b", "Bruno", AccountType.CURRENT, listOf(adultB.id))
    val joint = Account("acc-j", "Joint savings", AccountType.SAVINGS, listOf(adultA.id, adultB.id))
    val kid = Account("acc-c", "Carla", AccountType.SAVINGS, listOf(child.id))
    val mortgage = Account("acc-m", "Mortgage", AccountType.MORTGAGE, listOf(adultA.id, adultB.id))
    val groceries = Category("cat-g", "Groceries", "#2D55D6")
    val school = Category("cat-s", "School", "#D9652B")

    fun database(transactions: List<Transaction> = emptyList()) = FamilyDatabase(
        id = "db",
        createdAt = 0,
        updatedAt = 0,
        updatedBy = "test",
        settings = HouseholdSettings("Test family"),
        members = listOf(adultA, adultB, child),
        accounts = listOf(currentA, currentB, joint, kid, mortgage),
        categories = listOf(groceries, school),
        transactions = transactions,
    )
}

private var txCounter = 0

fun tx(
    date: String,
    amountMinor: Long,
    accountId: String = TestFamily.currentA.id,
    categoryId: String? = null,
    transfer: Boolean = false,
) = Transaction(
    id = "tx-${txCounter++}",
    accountId = accountId,
    date = LocalDate.parse(date),
    amountMinor = amountMinor,
    description = "test",
    categoryId = categoryId,
    isInternalTransfer = transfer,
)
