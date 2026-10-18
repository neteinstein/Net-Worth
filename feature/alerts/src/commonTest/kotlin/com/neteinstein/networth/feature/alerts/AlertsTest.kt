package com.neteinstein.networth.feature.alerts

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.SetSeasonalUseCase
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.alerts.domain.usecase.AccountName
import com.neteinstein.networth.feature.alerts.domain.usecase.AccountShare
import com.neteinstein.networth.feature.alerts.domain.usecase.BuildAlertsUseCase
import com.neteinstein.networth.feature.alerts.domain.usecase.GoodNews
import com.neteinstein.networth.feature.alerts.domain.usecase.Insight
import com.neteinstein.networth.feature.alerts.ui.screens.AlertsViewModel
import com.neteinstein.networth.feature.alerts.ui.screens.previewAlerts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * September 2026 against a baseline of September 2025 and June–August 2026 (four months with data).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertsTest {
    private val family = designFamily()
    private val pedro = family.accounts.first { it.name == "Pedro" }
    private val maria = family.accounts.first { it.name == "Maria" }
    private val kids = family.accounts.filter { it.type == AccountType.SAVINGS && it.ownerIds.size == 1 }

    private fun category(name: String) = family.categories.first { it.name == name }

    private val school = category("School")
    private val clothes = category("Clothes")
    private val dining = category("Dining & leisure")
    private val groceries = category("Groceries")
    private val transport = category("Transport")
    private val sep = CalendarMonth(2026, 9)

    private fun history(): FamilyDatabase {
        var n = 0

        fun tx(
            account: String,
            date: LocalDate,
            amount: Long,
            description: String,
            category: String? = null,
            sub: String? = null,
        ) = Transaction("t${n++}", account, date, amount, description, categoryId = category, subcategoryId = sub)

        fun d(
            month: Int,
            day: Int,
            year: Int = 2026,
        ) = LocalDate(year, month, day)
        val fuel = transport.subcategories.first { it.name == "Fuel" }.id
        val books = school.subcategories.first { it.name == "Books & materials" }.id
        val canteen = school.subcategories.first { it.name == "Canteen" }.id
        val txs =
            buildList {
                add(tx(maria.id, d(9, 5, 2025), -580_00, "LIVRARIA", school.id))
                for (m in 6..8) {
                    add(tx(maria.id, d(m, 3), -100_00, "LIVRARIA", school.id))
                    add(tx(pedro.id, d(m, 4), -300_00, "PINGO DOCE", groceries.id))
                    add(tx(pedro.id, d(m, 5), -300_00, "RESTAURANTE", dining.id))
                    add(tx(maria.id, d(m, 6), -100_00, "ZARA", clothes.id))
                    kids.forEach { add(tx(it.id, d(m, 1), 30_00, "MESADA")) }
                }
                add(tx(pedro.id, d(7, 10), -100_00, "GALP", transport.id, fuel))
                add(tx(pedro.id, d(8, 10), -120_00, "GALP", transport.id, fuel))
                add(tx(pedro.id, d(8, 8), -13_99, "Cobrança Sdd Disney Plus Adc 1", null))
                // September
                add(tx(pedro.id, d(9, 2), -300_00, "LIVRARIA", school.id, books))
                add(tx(maria.id, d(9, 9), -310_00, "CANTINA", school.id, canteen))
                add(tx(pedro.id, d(9, 4), -230_00, "PINGO DOCE", groceries.id))
                add(tx(pedro.id, d(9, 5), -100_00, "RESTAURANTE", dining.id))
                add(tx(maria.id, d(9, 2), -200_00, "ZARA", clothes.id))
                add(tx(pedro.id, d(9, 11), -20_00, "ZARA", clothes.id))
                add(tx(pedro.id, d(9, 10), -140_00, "GALP", transport.id, fuel))
                add(tx(pedro.id, d(9, 8), -14_49, "Cobrança Sdd Disney Plus Adc 2", null))
                add(tx(pedro.id, d(9, 20), -1_200_00, "IKEA", null))
                add(tx(pedro.id, d(9, 21), -1_500_00, "Pagamento Prestação Ch N° 1", null))
                kids.forEach { add(tx(it.id, d(9, 1), 30_00, "MESADA")) }
            }
        return family.copy(
            transactions = txs,
            categories = family.categories.map { if (it.id == transport.id) it.copy(warnOnSpike = false) else it },
            settings = family.settings.copy(alerts = family.settings.alerts.copy(largePaymentMinor = 1_000_00)),
        )
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun spikesExplainWhereTheMoneyWent() {
        val alerts = BuildAlertsUseCase()(history())
        assertEquals(sep, alerts.month)
        assertEquals(40, alerts.thresholdPct)
        assertEquals(listOf("School", "Clothes"), alerts.spikes.map { it.name })

        val schoolSpike = alerts.spikes[0]
        assertEquals(610_00, schoolSpike.spentMinor)
        assertEquals(220_00, schoolSpike.averageMinor)
        assertEquals(AccountShare.BothCurrent, schoolSpike.accounts)
        assertEquals(listOf("Canteen" to 310_00L, "Books & materials" to 300_00L), schoolSpike.parts)
        assertEquals(580_00, schoolSpike.sameMonthLastYearMinor)
        assertTrue(schoolSpike.repeatsEveryYear)
        assertEquals(2, schoolSpike.purchases)
        assertEquals(LocalDate(2026, 9, 2), schoolSpike.firstDay)
        assertEquals(LocalDate(2026, 9, 9), schoolSpike.lastDay)
        assertFalse(schoolSpike.seasonal)

        val clothesSpike = alerts.spikes[1]
        assertEquals(AccountShare.Mostly(AccountName("Maria", true)), clothesSpike.accounts)
        assertTrue(clothesSpike.parts.isEmpty())
        assertFalse(clothesSpike.repeatsEveryYear)
    }

    @Test
    fun worthALookAndGoingWell() {
        val alerts = BuildAlertsUseCase()(history())
        val recurring = alerts.worthALook.filterIsInstance<Insight.NewRecurring>().single()
        assertEquals("Disney Plus", recurring.name)
        assertEquals(14_49, recurring.amountMinor)
        assertEquals(LocalDate(2026, 8, 8), recurring.since)
        assertEquals(AccountName("Pedro", true), recurring.account)
        assertEquals(173_88, recurring.yearlyMinor)

        val rising = alerts.worthALook.filterIsInstance<Insight.Rising>().single()
        assertEquals("Fuel", rising.name)
        assertEquals(listOf(100_00L, 120_00L, 140_00L), rising.values)
        assertEquals(CalendarMonth(2026, 7), rising.first)

        val large = alerts.worthALook.filterIsInstance<Insight.LargePayment>().single()
        assertEquals("IKEA", large.description)
        assertEquals(1_200_00, large.amountMinor)

        assertEquals(GoodNews.UnderAverage("Dining & leisure", 55, 125_00), alerts.goingWell[0])
        assertEquals(GoodNews.SavingsStreak(3, 30_00, 4), alerts.goingWell.last())

        val quiet = history().let {
            it.copy(settings = it.settings.copy(alerts = it.settings.alerts.copy(newSubscriptions = false, largePaymentMinor = 0)))
        }
        val fewer = BuildAlertsUseCase()(quiet).worthALook
        assertTrue(fewer.none { it is Insight.NewRecurring || it is Insight.LargePayment })
    }

    @Test
    fun categoriesOverTheirBudgetAreWorthALook() {
        val budgets = mapOf(groceries.id to 200_00L, dining.id to 150_00L, school.id to 300_00L)
        val db = history().let { db -> db.copy(categories = db.categories.map { it.copy(monthlyBudgetMinor = budgets[it.id]) }) }
        val over = BuildAlertsUseCase()(db).worthALook.filterIsInstance<Insight.OverBudget>()
        // Groceries €230 of €200; dining is under; School is over but already has its spike card.
        assertEquals(listOf(Insight.OverBudget("Groceries", 230_00, 200_00)), over)
        assertEquals(30_00, over.single().overMinor)
        assertTrue(BuildAlertsUseCase()(history()).worthALook.none { it is Insight.OverBudget })
    }

    @Test
    fun emptyHistoryHasNoAlerts() {
        val alerts = BuildAlertsUseCase()(family)
        assertNull(alerts.month)
        assertTrue(alerts.spikes.isEmpty() && alerts.worthALook.isEmpty() && alerts.goingWell.isEmpty())
        assertEquals(2, previewAlerts().spikes.size)
    }

    @Test
    fun markingAsSeasonalKeepsTheSpikeAsPlannedAndCanBeUndone() =
        runTest {
            val repository = FakeDatabaseRepository.ready(history())
            val vm =
                AlertsViewModel(
                    ObserveDatabaseUseCase(repository),
                    BuildAlertsUseCase(),
                    SetSeasonalUseCase(EditDatabaseUseCase(repository)),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
            val spike = vm.state.value.alerts!!.spikes.first()

            vm.onToggleSeasonal(spike)
            val marked = vm.state.value.alerts!!.spikes.first()
            assertTrue(marked.seasonal)
            assertEquals(9, repository.database.category(school.id)!!.seasonalPeakMonth)

            vm.onToggleSeasonal(marked)
            assertFalse(vm.state.value.alerts!!.spikes.first().seasonal)
            assertTrue(repository.database.settings.alerts.seasonalMarks.isEmpty())

            repository.state.value = DatabaseState.NotSelected
            assertNull(vm.state.value.alerts)
        }
}
