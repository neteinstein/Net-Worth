package com.neteinstein.networth.feature.spending

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.model.TransactionFilter
import com.neteinstein.networth.core.domain.model.TransactionTypeFilter
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.spending.domain.model.DateRange
import com.neteinstein.networth.feature.spending.domain.model.PeriodKind
import com.neteinstein.networth.feature.spending.domain.model.TransactionQuery
import com.neteinstein.networth.feature.spending.domain.model.UNCATEGORISED
import com.neteinstein.networth.feature.spending.domain.model.activeCount
import com.neteinstein.networth.feature.spending.domain.model.adultIds
import com.neteinstein.networth.feature.spending.domain.model.find
import com.neteinstein.networth.feature.spending.domain.model.matches
import com.neteinstein.networth.feature.spending.domain.usecase.BuildSpendingUseCase
import com.neteinstein.networth.feature.spending.ui.PeriodLabels
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Spending maths on a small history: June–August as the baseline, September as the month shown. */
internal object History {
    val family = designFamily()
    val pedro = family.accounts.first { it.name == "Pedro" }
    val maria = family.accounts.first { it.name == "Maria" }
    val joint = family.accounts.first { it.type == AccountType.SAVINGS && it.ownerIds.size == 2 }
    val kids = family.accounts.filter { it.type == AccountType.SAVINGS && it.ownerIds.size == 1 }
    val groceries = family.categories.first { it.name == "Groceries" }
    val school = family.categories.first { it.name == "School" }

    fun d(
        month: Int,
        day: Int,
    ) = LocalDate(2026, month, day)

    fun database(): FamilyDatabase {
        var n = 0

        fun tx(
            account: String,
            date: LocalDate,
            amount: Long,
            description: String,
            category: String? = null,
            internal: Boolean = false,
            members: List<String> = emptyList(),
        ) = Transaction(
            "t${n++}",
            account,
            date,
            amount,
            description,
            categoryId = category,
            isInternalTransfer = internal,
            memberIds = members,
        )
        val history =
            (6..8).flatMap { m ->
                listOf(
                    tx(pedro.id, d(m, 5), -300_00, "PINGO DOCE", groceries.id),
                    tx(maria.id, d(m, 6), -100_00, "LIVRARIA", school.id),
                )
            }
        val inês = family.members.first { it.name == "Inês" }.id
        val september =
            listOf(
                tx(pedro.id, d(9, 10), -310_00, "PINGO DOCE", groceries.id),
                tx(maria.id, d(9, 3), -250_00, "LIVRARIA ESCOLAR", school.id, members = listOf(inês)),
                tx(pedro.id, d(9, 12), -20_00, "LOJA"),
                tx(pedro.id, d(9, 1), 2_000_00, "SALARIO"),
                tx(pedro.id, d(9, 2), -450_00, "TRF P/ POUPANCA", internal = true),
                tx(joint.id, d(9, 2), 450_00, "TRF DE PEDRO", internal = true),
            ) + kids.map { tx(it.id, d(9, 1), 30_00, "MESADA") }
        return family.copy(transactions = history + september)
    }
}

class SpendingTest {
    private val db = History.database()
    private val september = DateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
    private val build = BuildSpendingUseCase()

    @Test
    fun monthComparesEachCategoryWithItsAverage() {
        val spending = build(db, PeriodKind.MONTH, september, AccountScope.All)
        assertEquals(580_00, spending.totalMinor)
        assertEquals(116_00, spending.perPersonMinor)
        assertNull(spending.shareOfFamilyPercent)
        assertNull(spending.savings)
        assertEquals(listOf(History.groceries.id, History.school.id, null), spending.rows.map { it.categoryId })
        val (groceries, school, other) = spending.rows
        assertEquals(300_00, groceries.averageMinor)
        assertEquals(3, groceries.deltaPercent)
        assertFalse(groceries.isSpike)
        assertEquals(150, school.deltaPercent)
        assertTrue(school.isSpike)
        assertNull(other.averageMinor)
        assertNull(other.deltaPercent)
        assertEquals(310_00, spending.maxRowMinor)
        assertTrue(spending.scopeAccountIds.isEmpty())
    }

    @Test
    fun oneAccountShowsItsShareAndSavingsShowMoneyInAndOut() {
        val pedro = build(db, PeriodKind.MONTH, september, AccountScope.Single(History.pedro.id))
        assertEquals(330_00, pedro.totalMinor)
        assertEquals(56, pedro.shareOfFamilyPercent)
        assertNull(pedro.perPersonMinor)
        assertEquals("Pedro", pedro.scopeName)
        assertEquals(listOf(History.pedro.id), pedro.scopeAccountIds)
        assertEquals(listOf(History.groceries.id, null), pedro.rows.map { it.categoryId })

        val kids = build(db, PeriodKind.MONTH, september, AccountScope.Children)
        assertEquals(90_00, kids.savings!!.inMinor)
        assertEquals(0, kids.savings!!.outMinor)
        assertTrue(kids.rows.isEmpty())
        val joint = build(db, PeriodKind.MONTH, september, AccountScope.Single(History.joint.id))
        assertEquals(450_00, joint.savings!!.inMinor)

        assertEquals(AccountScope.All, build(db, PeriodKind.MONTH, september, AccountScope.Single("gone")).scope)
    }

    @Test
    fun weeksAndYearsScaleTheAverage() {
        val week = DateRange.of(PeriodKind.WEEK, LocalDate(2026, 9, 10))
        val spending = build(db, PeriodKind.WEEK, week, AccountScope.All)
        assertEquals(LocalDate(2026, 9, 7), week.start)
        assertEquals(
            (300_00 * (7 / BuildSpendingUseCase.DAYS_PER_MONTH)).toLong(),
            spending.rows.first {
                it.categoryId ==
                    History.groceries.id
            }.averageMinor,
        )
        val year = build(db, PeriodKind.YEAR, DateRange.of(PeriodKind.YEAR, LocalDate(2027, 3, 1)), AccountScope.All)
        // Baseline for 2027 is June–September 2026: (3 × 300 + 310) / 4 per month, × 12.
        assertEquals(3_630_00, year.rows.first { it.categoryId == History.groceries.id }.averageMinor)
        assertEquals(0, year.totalMinor)
    }

    @Test
    fun periodsCoverWeeksMonthsYearsAndMove() {
        val month = DateRange.of(PeriodKind.MONTH, LocalDate(2026, 2, 14))
        assertEquals(DateRange(LocalDate(2026, 2, 1), LocalDate(2026, 2, 28)), month)
        assertEquals(28, month.days)
        assertTrue(LocalDate(2026, 2, 28) in month)
        assertEquals(DateRange(LocalDate(2026, 3, 1), LocalDate(2026, 3, 31)), month.shift(PeriodKind.MONTH, 1))
        assertEquals(
            DateRange(LocalDate(2025, 1, 1), LocalDate(2025, 12, 31)),
            DateRange.of(PeriodKind.YEAR, LocalDate(2026, 5, 1)).shift(PeriodKind.YEAR, -1),
        )
        val week = DateRange.of(PeriodKind.WEEK, LocalDate(2026, 9, 13))
        assertEquals(LocalDate(2026, 9, 7), week.start)
        assertEquals(LocalDate(2026, 9, 14), week.shift(PeriodKind.WEEK, 1).start)
        val custom = DateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 10))
        assertEquals(DateRange(LocalDate(2026, 9, 11), LocalDate(2026, 9, 20)), custom.shift(PeriodKind.CUSTOM, 1))
        assertEquals(DateRange.of(PeriodKind.MONTH, LocalDate(2026, 9, 3)), DateRange.of(PeriodKind.CUSTOM, LocalDate(2026, 9, 3)))
        assertFailsWith<IllegalArgumentException> { DateRange(LocalDate(2026, 9, 2), LocalDate(2026, 9, 1)) }
    }

    @Test
    fun filtersMatchEveryCriterion() {
        val inês = History.family.members.first { it.name == "Inês" }.id

        fun count(filter: TransactionFilter) = db.find(TransactionQuery(filter, september)).size
        assertEquals(3, count(TransactionFilter()))
        assertEquals(4, count(TransactionFilter(includeTransfers = true)))
        assertEquals(1 + 3 + 1, count(TransactionFilter(type = TransactionTypeFilter.INCOME, includeTransfers = true)))
        assertEquals(7, count(TransactionFilter(type = TransactionTypeFilter.BOTH)))
        assertEquals(2, count(TransactionFilter(accountIds = listOf(History.pedro.id))))
        assertEquals(1, count(TransactionFilter(categoryIds = listOf(UNCATEGORISED))))
        assertEquals(1, count(TransactionFilter(memberIds = listOf(inês))))
        assertEquals(2, count(TransactionFilter(minMinor = 100_00)))
        assertEquals(1, count(TransactionFilter(maxMinor = 100_00)))
        assertEquals(1, count(TransactionFilter(text = "  escolar ")))
        assertEquals(3, count(TransactionFilter(text = " ")))
        val noted = db.transactions.first().copy(note = "Birthday")
        assertTrue(TransactionFilter(type = TransactionTypeFilter.BOTH, text = "birth").matches(noted))
        assertEquals("t8", db.find(TransactionQuery(TransactionFilter())).first().id) // newest first
        assertEquals(0, TransactionFilter().activeCount)
        assertEquals(
            7,
            TransactionFilter(listOf("a"), listOf("c"), listOf("m"), TransactionTypeFilter.BOTH, true, 1, null, "x").activeCount,
        )
        assertEquals(2, db.adultIds().size)
    }

    @Test
    fun periodLabels() {
        val short = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val long = short.map { "$it-long" }
        assertEquals("Sep 2026", PeriodLabels.title(PeriodKind.MONTH, september, short))
        assertEquals("Sep-long", PeriodLabels.inline(PeriodKind.MONTH, september, short, long))
        val year = DateRange.of(PeriodKind.YEAR, september.start)
        assertEquals("2026", PeriodLabels.title(PeriodKind.YEAR, year, short))
        assertEquals("2026", PeriodLabels.inline(PeriodKind.YEAR, year, short, long))
        val week = DateRange(LocalDate(2026, 9, 22), LocalDate(2026, 9, 28))
        assertEquals("22–28 Sep 2026", PeriodLabels.title(PeriodKind.WEEK, week, short))
        assertEquals("22–28 Sep", PeriodLabels.inline(PeriodKind.WEEK, week, short, long))
        assertEquals(
            "28 Sep – 4 Oct 2026",
            PeriodLabels.title(PeriodKind.CUSTOM, DateRange(LocalDate(2026, 9, 28), LocalDate(2026, 10, 4)), short),
        )
        assertEquals(
            "28 Dec 2025 – 4 Jan 2026",
            PeriodLabels.title(PeriodKind.CUSTOM, DateRange(LocalDate(2025, 12, 28), LocalDate(2026, 1, 4)), short),
        )
        assertEquals("3 Sep 2026", PeriodLabels.title(PeriodKind.CUSTOM, DateRange(LocalDate(2026, 9, 3), LocalDate(2026, 9, 3)), short))
    }
}
