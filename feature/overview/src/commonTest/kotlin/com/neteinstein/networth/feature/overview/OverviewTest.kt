package com.neteinstein.networth.feature.overview

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.BalanceSnapshot
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.SyncDatabaseUseCase
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.FixedClock
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.overview.domain.model.OverviewPeriod
import com.neteinstein.networth.feature.overview.domain.model.TrendStep
import com.neteinstein.networth.feature.overview.domain.usecase.BuildOverviewUseCase
import com.neteinstein.networth.feature.overview.domain.usecase.MarkSeasonalUseCase
import com.neteinstein.networth.feature.overview.ui.screens.OverviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewTest {
    private val sep = CalendarMonth(2026, 9)

    private fun build(db: FamilyDatabase, period: OverviewPeriod, scope: AccountScope = AccountScope.All) =
        BuildOverviewUseCase()(db, period, scope, today = LocalDate(2026, 10, 18))

    /** The design family with three months of history: School spikes in September. */
    private fun history(): FamilyDatabase {
        val base = designFamily()
        val pedro = base.accounts.first { it.name == "Pedro" }
        val joint = base.accounts.first { it.name == "Joint savings" }
        val school = base.categories.first { it.name == "School" }
        val groceries = base.categories.first { it.name == "Groceries" }
        var n = 0

        fun tx(date: String, amount: Long, category: String? = null, account: String = pedro.id, transfer: Boolean = false) =
            Transaction("t${n++}", account, LocalDate.parse(date), amount, "x", categoryId = category, isInternalTransfer = transfer)
        val months = listOf("2026-07", "2026-08", "2026-09")
        val transactions = months.flatMap { m ->
            listOf(
                tx("$m-01", 4_850_00),
                tx("$m-05", -900_00, groceries.id),
                tx("$m-10", if (m == "2026-09") -610_00 else -285_00, school.id),
                tx("$m-20", -450_00, transfer = true),
                tx("$m-20", 450_00, account = joint.id, transfer = true),
            )
        } + tx("2026-09-21", -12_00)
        return base.copy(
            transactions = transactions,
            balanceSnapshots = listOf(BalanceSnapshot("b", joint.id, LocalDate(2026, 6, 30), 12_600_00)),
        )
    }

    @Test
    fun buildsTheOverviewForAMonth() {
        val overview = build(history(), OverviewPeriod.Month(sep))

        assertEquals(2, overview.adults)
        assertEquals(3, overview.children)
        assertEquals(4_850_00, overview.incomeMinor)
        assertEquals(1_522_00, overview.spentMinor)
        assertEquals(3_328_00, overview.savedMinor)
        assertEquals(68, overview.savedPercent)
        assertEquals(listOf("Groceries", "School", null), overview.shares.map { it.name })
        assertEquals(listOf(59, 40, 0), overview.shares.map { it.percent })
        assertTrue(overview.otherShares.isEmpty())
        assertEquals("School", overview.spike?.categoryName)
        assertEquals(6, overview.trend.size)
        assertEquals(listOf(0L, 0L, 0L, 1_185_00L, 1_185_00L, 1_522_00L), overview.trend.map { it.spentMinor })
        assertTrue(overview.trend.last().isSelected)
        assertEquals(1_185_00, overview.averageSpentMinor)
        assertEquals(listOf(sep, sep.previous(), sep.plus(-2)), overview.months)
        assertEquals(listOf(2026), overview.years)
        assertEquals(LocalDate(2026, 9, 1) to LocalDate(2026, 9, 30), overview.from to overview.to)
        assertTrue(overview.hasTransactions)
        // Pedro's account: 3 × (4850 − 900 − school − 450) − 12; joint: 12,600 + 3 × 450.
        val pedro = overview.accounts.first { it.name == "Pedro" }
        assertEquals(3 * 4_850_00 - 3 * 900_00 - 285_00 * 2 - 610_00 - 3 * 450_00 - 12_00, pedro.balanceMinor)
        assertEquals(13_950_00, overview.accounts.first { it.name == "Joint savings" }.balanceMinor)
        assertEquals(overview.accounts.sumOf { it.balanceMinor }, overview.netWorthMinor)
        assertNotNull(overview.projectedSavingsMinor)
        assertTrue(overview.accounts.first { it.name == "Inês" }.isChild)
    }

    @Test
    fun scopesNarrowTheFiguresAndUnknownScopesFallBack() {
        val db = history()
        val joint = db.accounts.first { it.name == "Joint savings" }

        val jointOnly = build(db, OverviewPeriod.Month(sep), AccountScope.Single(joint.id))
        val unknown = build(db, OverviewPeriod.Month(sep), AccountScope.Single("nope"))

        assertEquals(0, jointOnly.spentMinor)
        assertNull(jointOnly.spike)
        assertEquals(AccountScope.All, unknown.scope)
        assertEquals(AccountScope.Children, build(db, OverviewPeriod.Month(sep)).scopes.last())
    }

    @Test
    fun emptyDatabaseHasNoHistory() {
        val overview = build(designFamily(), OverviewPeriod.Month(sep))

        assertFalse(overview.hasTransactions)
        assertEquals(0, overview.needsReview)
        assertNull(overview.projectedSavingsMinor)
        assertNull(overview.savedPercent)
        assertTrue(overview.shares.isEmpty())
        assertEquals(listOf(sep), overview.months)
    }

    @Test
    fun allTimeAddsUpEveryMonthWithoutASpike() {
        val overview = build(history(), OverviewPeriod.AllTime)

        assertEquals(LocalDate(2026, 7, 1), overview.from)
        assertEquals(LocalDate(2026, 9, 21), overview.to)
        assertEquals(sep, overview.month)
        assertEquals(3 * 4_850_00, overview.incomeMinor)
        // Groceries 3 × 900, school 285 + 285 + 610, 12 uncategorised; transfers are not spending.
        assertEquals(3_892_00, overview.spentMinor)
        assertEquals(listOf(2_700_00L, 1_180_00L, 12_00L), overview.shares.map { it.amountMinor })
        assertNull(overview.spike)
        assertEquals(3_892_00 / 3, overview.averageSpentMinor)
        assertEquals(listOf(TrendStep.MONTH), overview.trend.map { it.step }.distinct())
        assertEquals(listOf(1_185_00L, 1_185_00L, 1_522_00L), overview.trend.map { it.spentMinor })
        assertTrue(overview.trend.none { it.isSelected })
        assertNotNull(overview.projectedSavingsMinor)
    }

    @Test
    fun aYearAndACustomRangeCoverExactlyTheirDays() {
        val year = build(history(), OverviewPeriod.Year(2025))
        val custom = build(history(), OverviewPeriod.Custom(LocalDate(2026, 8, 15), LocalDate(2026, 9, 15)))

        assertEquals(0, year.spentMinor)
        assertEquals(listOf(2026, 2025), year.years)
        assertEquals(LocalDate(2025, 12, 31), year.to)
        assertEquals(4_850_00, custom.incomeMinor)
        assertEquals(1_510_00, custom.spentMinor)
        assertEquals(listOf(CalendarMonth(2026, 8), sep), custom.trend.map { it.month })
        assertEquals(listOf(sep, sep.previous(), sep.plus(-2)), custom.months)
    }

    @Test
    fun longPeriodsGroupTheTrendByQuarterThenByYear() {
        val quarters = build(history(), OverviewPeriod.Custom(LocalDate(2025, 10, 1), LocalDate(2026, 9, 30)))
        val years = build(history(), OverviewPeriod.Custom(LocalDate(2019, 1, 1), LocalDate(2026, 9, 30)))

        assertEquals(List(4) { TrendStep.QUARTER }, quarters.trend.map { it.step })
        assertEquals(listOf(0L, 0L, 0L, 3_892_00L), quarters.trend.map { it.spentMinor })
        assertEquals(CalendarMonth(2026, 7), quarters.trend.last().month)
        assertEquals((2021..2026).toList(), years.trend.map { it.month.year })
        assertEquals(TrendStep.YEAR, years.trend.first().step)
        assertEquals(3_892_00, years.trend.last().spentMinor)
    }

    @Test
    fun allTimeWithoutMovementsIsToday() {
        val overview = build(designFamily(), OverviewPeriod.AllTime)

        assertEquals(LocalDate(2026, 10, 18), overview.from)
        assertEquals(LocalDate(2026, 10, 18), overview.to)
        assertTrue(overview.months.isEmpty())
        assertEquals(0, overview.averageSpentMinor)
    }

    private val repository = FakeDatabaseRepository.ready(history())

    private fun TestScope.viewModel() = OverviewViewModel(
        observeDatabase = ObserveDatabaseUseCase(repository),
        buildOverview = BuildOverviewUseCase(),
        markSeasonal = MarkSeasonalUseCase(EditDatabaseUseCase(repository)),
        syncDatabase = SyncDatabaseUseCase(repository),
        clock = FixedClock(),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun followsTheLatestMonthUntilOneIsPicked() = runTest {
        val vm = viewModel()
        assertEquals(sep, vm.state.value.overview?.month)

        vm.onMonthSelected(CalendarMonth(2026, 8))
        vm.onScopeSelected(AccountScope.Children)

        assertEquals(CalendarMonth(2026, 8), vm.state.value.overview?.month)
        assertEquals(AccountScope.Children, vm.state.value.overview?.scope)
    }

    @Test
    fun picksAggregatePeriodsAndCustomRangesInEitherOrder() = runTest {
        val vm = viewModel()

        vm.onPeriodSelected(OverviewPeriod.AllTime)
        assertEquals(OverviewPeriod.AllTime, vm.state.value.overview?.period)

        vm.onCustomRange(LocalDate(2026, 9, 15), LocalDate(2026, 8, 15))
        assertEquals(OverviewPeriod.Custom(LocalDate(2026, 8, 15), LocalDate(2026, 9, 15)), vm.state.value.overview?.period)
    }

    @Test
    fun markingSeasonalNeedsAMonth() = runTest {
        val vm = viewModel()
        val school = vm.state.value.overview!!.spike!!.categoryId
        vm.onPeriodSelected(OverviewPeriod.AllTime)

        vm.onMarkSeasonal(school)

        assertNull(repository.database.category(school)?.seasonalPeakMonth)
    }

    @Test
    fun emptyDatabaseStartsAtTodaysMonth() = runTest {
        repository.state.value = FakeDatabaseRepository.ready(designFamily()).state.value

        assertEquals(CalendarMonth(2026, 10), viewModel().state.value.overview?.month)
    }

    @Test
    fun markingSeasonalSilencesTheSpike() = runTest {
        val vm = viewModel()
        val school = vm.state.value.overview!!.spike!!.categoryId

        vm.onMarkSeasonal(school)

        assertNull(vm.state.value.overview?.spike)
        assertEquals(9, repository.database.category(school)?.seasonalPeakMonth)
        assertEquals(1, repository.database.settings.alerts.seasonalMarks.size)
    }

    @Test
    fun syncNowDelegates() = runTest {
        viewModel().onSyncNow()

        assertEquals(listOf("sync"), repository.calls)
    }

    @Test
    fun noDatabaseMeansNoOverview() = runTest {
        repository.state.value = com.neteinstein.networth.core.domain.repository.DatabaseState.Loading

        assertNull(viewModel().state.value.overview)
    }
}
