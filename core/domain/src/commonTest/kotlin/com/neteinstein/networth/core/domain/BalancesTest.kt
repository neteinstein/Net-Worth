package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.balanceOf
import com.neteinstein.networth.core.domain.calc.isChildAccount
import com.neteinstein.networth.core.domain.calc.netWorth
import com.neteinstein.networth.core.domain.calc.savingsBalance
import com.neteinstein.networth.core.domain.calc.scopeChoices
import com.neteinstein.networth.core.domain.model.BalanceSnapshot
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BalancesTest {
    private val db = TestFamily.database(
        listOf(
            tx("2026-08-20", -50_00),
            tx("2026-09-02", -100_00),
            tx("2026-09-10", 1_000_00),
            tx("2026-09-12", 30_00, accountId = TestFamily.kid.id),
        ),
    ).copy(
        balanceSnapshots = listOf(
            BalanceSnapshot("s1", TestFamily.currentA.id, LocalDate(2026, 9, 1), 4_394_37),
            BalanceSnapshot("s2", TestFamily.mortgage.id, LocalDate(2026, 9, 30), 147_268_16),
            BalanceSnapshot("s3", TestFamily.joint.id, LocalDate(2026, 9, 30), 12_600_00),
        ),
    )

    @Test
    fun balanceIsLatestSnapshotPlusLaterMovements() {
        assertEquals(5_294_37, db.balanceOf(TestFamily.currentA))
        assertEquals(4_294_37, db.balanceOf(TestFamily.currentA, asOf = LocalDate(2026, 9, 5)))
        assertEquals(-50_00, db.balanceOf(TestFamily.currentA, asOf = LocalDate(2026, 8, 31)))
        assertEquals(30_00, db.balanceOf(TestFamily.kid))
    }

    @Test
    fun netWorthSubtractsLiabilities() {
        val netWorth = db.netWorth()

        assertEquals(5_294_37 + 12_600_00 + 30_00, netWorth.assetsMinor)
        assertEquals(147_268_16, netWorth.liabilitiesMinor)
        assertEquals(netWorth.assetsMinor - netWorth.liabilitiesMinor, netWorth.netMinor)
    }

    @Test
    fun savingsBalanceCountsOnlySavingsAccounts() {
        assertEquals(12_630_00, db.savingsBalance())
        assertEquals(30_00, db.savingsBalance(AccountScope.Children))
    }

    @Test
    fun scopeChoicesGroupChildrenAndHideDebt() {
        assertTrue(db.isChildAccount(TestFamily.kid))
        assertFalse(db.isChildAccount(TestFamily.joint))
        assertEquals(
            listOf(
                AccountScope.All,
                AccountScope.Single(TestFamily.currentA.id),
                AccountScope.Single(TestFamily.currentB.id),
                AccountScope.Single(TestFamily.joint.id),
                AccountScope.Children,
            ),
            db.scopeChoices(),
        )
    }
}
