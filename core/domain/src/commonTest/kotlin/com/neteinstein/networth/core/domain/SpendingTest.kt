package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.TestFamily.groceries
import com.neteinstein.networth.core.domain.TestFamily.school
import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.coveredMonths
import com.neteinstein.networth.core.domain.calc.latestMonth
import com.neteinstein.networth.core.domain.calc.monthlySpending
import com.neteinstein.networth.core.domain.calc.totals
import com.neteinstein.networth.core.domain.calc.trailingAverage
import com.neteinstein.networth.core.domain.model.CalendarMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpendingTest {
    private val sep = CalendarMonth(2026, 9)

    @Test
    fun separatesIncomeSpendingRefundsAndTransfers() {
        val db = TestFamily.database(
            listOf(
                tx("2026-09-01", 4_850_00),
                tx("2026-09-03", -920_00, categoryId = groceries.id),
                tx("2026-09-04", 20_00, categoryId = groceries.id), // refund
                tx("2026-09-05", -45_00), // uncategorised outflow
                tx("2026-09-06", -450_00, transfer = true),
                tx("2026-09-06", 450_00, accountId = TestFamily.joint.id, transfer = true),
                tx("2026-10-01", -1_00, categoryId = groceries.id), // other month
            ),
        )

        val totals = db.totals(AccountScope.All, sep)

        assertEquals(4_850_00, totals.incomeMinor)
        assertEquals(900_00, totals.spentByCategory[groceries.id])
        assertEquals(45_00, totals.spentByCategory[null])
        assertEquals(945_00, totals.spentMinor)
        assertEquals(3_905_00, totals.savedMinor)
        assertEquals(80, totals.savedPercent)
    }

    @Test
    fun scopesFilterAccounts() {
        val db = TestFamily.database(
            listOf(
                tx("2026-09-03", -100_00, categoryId = groceries.id),
                tx("2026-09-03", -40_00, accountId = TestFamily.currentB.id, categoryId = groceries.id),
                tx("2026-09-03", 30_00, accountId = TestFamily.kid.id),
            ),
        )

        assertEquals(140_00, db.totals(AccountScope.All, sep).spentMinor)
        assertEquals(40_00, db.totals(AccountScope.Single(TestFamily.currentB.id), sep).spentMinor)
        assertEquals(30_00, db.totals(AccountScope.Children, sep).incomeMinor)
        assertNull(db.totals(AccountScope.Children, sep).savedPercent?.takeIf { false })
    }

    @Test
    fun trailingAverageSkipsMonthsWithoutData() {
        val db = TestFamily.database(
            listOf(
                tx("2026-06-10", -300_00, categoryId = school.id),
                tx("2026-08-10", -200_00, categoryId = school.id),
                tx("2026-09-10", -610_00, categoryId = school.id),
            ),
        )

        val average = db.trailingAverage(AccountScope.All, sep)

        assertEquals(250_00, average.spentByCategory[school.id])
        assertEquals(setOf(CalendarMonth(2026, 6), CalendarMonth(2026, 8), sep), db.coveredMonths())
        assertEquals(sep, db.latestMonth())
    }

    @Test
    fun trailingAverageIsEmptyWithoutHistory() {
        assertEquals(0, TestFamily.database().trailingAverage(AccountScope.All, sep).spentMinor)
        assertNull(TestFamily.database().latestMonth())
    }

    @Test
    fun monthlySpendingListsOldestFirst() {
        val db = TestFamily.database(listOf(tx("2026-08-10", -200_00, categoryId = school.id)))

        val series = db.monthlySpending(AccountScope.All, sep, 3)

        assertEquals(listOf(CalendarMonth(2026, 7), CalendarMonth(2026, 8), sep), series.map { it.first })
        assertEquals(listOf(0L, 200_00L, 0L), series.map { it.second })
    }
}
