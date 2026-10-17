package com.neteinstein.networth.feature.future

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.BalanceSnapshot
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.SeasonalMark
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.future.domain.model.FutureScope
import com.neteinstein.networth.feature.future.domain.model.Trend
import com.neteinstein.networth.feature.future.domain.model.WhatIfs
import com.neteinstein.networth.feature.future.domain.usecase.BuildForecastUseCase
import com.neteinstein.networth.feature.future.domain.usecase.BuildFutureUseCase
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Twelve invented months, Nov 2025 – Oct 2026. Every month Pedro earns €4,000 and spends €2,000 on
 * groceries, Maria spends €500 on dining, €500 goes to joint savings and €30 to each child. December
 * also costs €1,500 in gifts ("Other"). So: €2,625 spent and €1,375 saved a month on average.
 */
class FutureTest {
    private val family = designFamily()
    private val pedro = family.accounts.first { it.name == "Pedro" }
    private val maria = family.accounts.first { it.name == "Maria" }
    private val joint = family.accounts.first { it.type == AccountType.SAVINGS && it.ownerIds.size == 2 }
    private val kids = family.accounts.filter { it.type == AccountType.SAVINGS && it.ownerIds.size == 1 }
    private val groceries = family.categories.first { it.name == "Groceries" }
    private val dining = family.categories.first { it.name == "Dining & leisure" }
    private val other = family.categories.first { it.name == "Other" }
    private val oct = CalendarMonth(2026, 10)

    private fun history(): FamilyDatabase {
        var n = 0

        fun tx(account: String, date: LocalDate, amount: Long, category: String? = null, transfer: Boolean = false) =
            Transaction("t${n++}", account, date, amount, "MOVEMENT", categoryId = category, isInternalTransfer = transfer)

        val txs = buildList {
            for (i in 0 until 12) {
                val month = CalendarMonth(2025, 11).plus(i)

                fun day(d: Int) = LocalDate(month.year, month.month, d)
                add(tx(pedro.id, day(1), 4_000_00))
                add(tx(pedro.id, day(5), -2_000_00, groceries.id))
                add(tx(maria.id, day(6), -500_00, dining.id))
                add(tx(pedro.id, day(2), -500_00, transfer = true))
                add(tx(joint.id, day(2), 500_00, transfer = true))
                add(tx(maria.id, day(3), -90_00, transfer = true))
                kids.forEach { add(tx(it.id, day(3), 30_00, transfer = true)) }
                if (month.month == 12) add(tx(pedro.id, day(20), -1_500_00, other.id))
            }
        }
        val start = LocalDate(2025, 10, 31)
        val snapshots = listOf(BalanceSnapshot("s-joint", joint.id, start, 10_000_00)) +
            kids.map { BalanceSnapshot("s-${it.id}", it.id, start, 1_000_00) }
        return family.copy(transactions = txs, balanceSnapshots = snapshots)
    }

    private val future = BuildFutureUseCase()
    private val forecast = BuildForecastUseCase()

    @Test
    fun allSavingsGrowByWhatTheFamilySaves() {
        val result = future(history())
        assertEquals(oct, result.latest)
        assertEquals(listOf(FutureScope.ALL, FutureScope.JOINT, FutureScope.KIDS), result.scopes)
        assertEquals(1_375_00, result.monthlyMinor)
        // Joint €16,000 + three children at €1,360.
        assertEquals(20_080_00, result.flat[0])
        assertEquals(36_580_00, result.flat[1])
        assertEquals(20_080_00 + 10 * 12 * 1_375_00, result.flat[10])
        assertTrue(result.withReturn[10] > result.flat[10])
        assertEquals(3.0, result.returnRatePct)
        assertTrue(result.kids.isEmpty())
    }

    @Test
    fun whatIfsAddToAllAndJointButNotKids() {
        val ifs = WhatIfs(cutDining = true, addExtra = true)
        val all = future(history(), FutureScope.ALL, ifs)
        assertEquals(100_00, all.diningCutMinor)
        assertEquals(100_00, all.extraMinor)
        assertEquals(1_575_00, all.monthlyMinor)
        assertEquals(700_00, future(history(), FutureScope.JOINT, ifs).monthlyMinor)
        assertEquals(500_00, future(history(), FutureScope.JOINT).monthlyMinor)
        assertEquals(90_00, future(history(), FutureScope.KIDS, ifs).monthlyMinor)
        // Setting money aside for a peak only smooths spending.
        assertEquals(1_375_00, future(history(), FutureScope.ALL, WhatIfs(setAside = true)).monthlyMinor)
    }

    @Test
    fun kidsAreProjectedOneByOneWithTheReturn() {
        val result = future(history(), FutureScope.KIDS)
        assertEquals(3 * 1_360_00L, result.flat[0])
        assertEquals(listOf("Inês", "Tomás", "Rita"), result.kids.map { it.name })
        assertEquals(1_360_00, result.kids[0].nowMinor)
        assertEquals(1_760_80, result.kids[0].withReturn[1])
    }

    @Test
    fun emergencyFundCountsMonthsOfSpendingAgainstJointSavings() {
        val covered = future(history()).emergencyFund!!
        assertEquals(6, covered.months)
        assertEquals(15_750_00, covered.targetMinor)
        assertEquals(16_000_00, covered.currentMinor)
        assertTrue(covered.covered)
        assertNull(covered.reachedOn)
        assertEquals(1f, covered.fraction)

        val db = history().let {
            it.copy(settings = it.settings.copy(assumptions = it.settings.assumptions.copy(emergencyFundMonths = 12)))
        }
        val fund = future(db).emergencyFund!!
        assertFalse(fund.covered)
        // €15,500 short at €500 a month: 31 months after Oct 2026.
        assertEquals(CalendarMonth(2029, 5), fund.reachedOn)
        // The extra €100 a month gets there sooner.
        assertEquals(CalendarMonth(2028, 12), future(db, whatIfs = WhatIfs(addExtra = true)).emergencyFund!!.reachedOn)
    }

    @Test
    fun tenYearSpendWithAndWithoutPriceRises() {
        val result = future(history())
        assertEquals(315_000_00, result.tenYearSpendMinor)
        assertTrue(result.tenYearSpendWithPricesMinor in 352_000_00..354_000_00)
        assertEquals(2.5, result.priceRisePct)
    }

    @Test
    fun setAsideSpreadsTheSeasonalExcessOverTheYear() {
        assertNull(future(history()).setAside)
        val db = history().let { db ->
            db.copy(
                categories = db.categories.map {
                    if (it.id ==
                        other.id
                    ) {
                        it.copy(seasonalPeakMonth = 12)
                    } else {
                        it
                    }
                },
            )
        }
        val aside = future(db).setAside!!
        assertEquals("Other", aside.categoryName)
        assertEquals(12, aside.month)
        assertEquals(125_00, aside.monthlyMinor)

        val marked = history().let { db ->
            db.copy(settings = db.settings.copy(alerts = db.settings.alerts.copy(seasonalMarks = listOf(SeasonalMark(other.id, 12, 2025)))))
        }
        assertEquals(125_00, future(marked).setAside!!.monthlyMinor)
    }

    @Test
    fun withoutHistoryNothingIsProjected() {
        val result = future(family, FutureScope.KIDS)
        assertNull(result.latest)
        assertEquals(FutureScope.KIDS, result.scope)
        assertEquals(0, result.monthlyMinor)
        assertNull(result.diningCutMinor)
        assertNull(result.emergencyFund)
        assertTrue(forecast(family).months.isEmpty())
        assertNull(forecast(family).changePercent)
    }

    @Test
    fun unavailableScopesFallBackToAll() {
        val noKids = history().let { db -> db.copy(accounts = db.accounts.filter { it !in kids }) }
        val result = future(noKids, FutureScope.KIDS)
        assertEquals(FutureScope.ALL, result.scope)
        assertEquals(listOf(FutureScope.ALL, FutureScope.JOINT), result.scopes)
        assertEquals(AccountScope.All, forecast(history(), AccountScope.Single("nope")).scope)
    }

    @Test
    fun yearAheadFollowsLastYearsMonthsWithPriceRises() {
        val result = forecast(history())
        assertEquals((1..12).map { oct.plus(it) }, result.months.map { it.month })
        assertEquals(4_100_00, result.months[1].spendMinor)
        assertEquals(2_562_50, result.months[0].spendMinor)
        assertEquals(4_000_00, result.monthlyIncomeMinor)
        assertEquals(listOf(CalendarMonth(2026, 12)), result.months.filter { it.overIncome }.map { it.month })
        assertEquals(32_287_50, result.yearSpendMinor)
        assertEquals(31_500_00, result.lastYearSpendMinor)
        assertEquals(2, result.changePercent)
        assertEquals(48_000_00 - 32_287_50, result.yearSaveMinor)
        assertEquals(16_500_00, result.lastYearSaveMinor)
        assertNull(result.shareOfFamilyPercent)
    }

    @Test
    fun peaksAndSettingMoneyAside() {
        val result = forecast(history())
        assertEquals(1, result.peaks.size)
        assertEquals(CalendarMonth(2026, 12), result.peaks[0].month)
        assertEquals("Other", result.peaks[0].reason)
        // €100 over income in December, spread over 12 months and rounded up to €10.
        assertEquals(10_00, result.setAsideMinor)
        assertEquals(listOf(CalendarMonth(2026, 12)), result.coveredMonths)

        val db = history().let { db ->
            db.copy(
                categories = db.categories.map {
                    if (it.id ==
                        groceries.id
                    ) {
                        it.copy(seasonalPeakMonth = 12)
                    } else {
                        it
                    }
                },
            )
        }
        assertEquals("Groceries", forecast(db).peaks[0].reason)
        val marked = history().let { db ->
            db.copy(
                settings = db.settings.copy(alerts = db.settings.alerts.copy(seasonalMarks = listOf(SeasonalMark(dining.id, 12, 2025)))),
            )
        }
        assertEquals("Dining & leisure", forecast(marked).peaks[0].reason)
    }

    @Test
    fun oneAccountShowsItsShareOfTheFamily() {
        val result = forecast(history(), AccountScope.Single(pedro.id))
        assertEquals(AccountScope.Single(pedro.id), result.scope)
        assertNull(result.monthlyIncomeMinor)
        assertNull(result.yearSaveMinor)
        assertNull(result.setAsideMinor)
        assertTrue(result.months.none { it.overIncome })
        assertEquals(26_137_50, result.yearSpendMinor)
        assertEquals(80, result.shareOfFamilyPercent)
    }

    @Test
    fun yearByYearGrowsIncomeAndPrices() {
        val result = forecast(history())
        assertEquals((2027..2031).toList(), result.years.map { it.year })
        assertEquals(32_287_50, result.years[0].spendMinor)
        assertEquals(15_712_50, result.years[0].saveMinor)
        assertEquals(20_080_00 + 15_712_50, result.years[0].balanceMinor)
        assertEquals(Trend.GROWING, result.trend)
        assertTrue(result.trendPerYearMinor > 0)

        fun withPrices(pct: Double) = history().let { db ->
            db.copy(settings = db.settings.copy(assumptions = db.settings.assumptions.copy(priceRisePct = pct, incomeGrowthPct = 0.0)))
        }
        assertEquals(Trend.SHRINKING, forecast(withPrices(5.0)).trend)
        assertEquals(Trend.FLAT, forecast(withPrices(0.0)).trend)
    }
}
