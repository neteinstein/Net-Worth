package com.neteinstein.networth.feature.future

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.future.domain.model.FutureScope
import com.neteinstein.networth.feature.future.domain.usecase.BuildForecastUseCase
import com.neteinstein.networth.feature.future.domain.usecase.BuildFutureUseCase
import com.neteinstein.networth.feature.future.ui.FutureFormat
import com.neteinstein.networth.feature.future.ui.screens.FutureUiState
import com.neteinstein.networth.feature.future.ui.screens.FutureViewModel
import com.neteinstein.networth.feature.future.ui.screens.YearAheadViewModel
import com.neteinstein.networth.feature.future.ui.screens.previewFutureState
import com.neteinstein.networth.feature.future.ui.screens.previewYearAheadState
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FutureViewModelsTest {
    private val family = designFamily()
    private val pedro = family.accounts.first { it.name == "Pedro" }
    private val gifts = family.categories.first { it.name == "Other" }

    /** Two months: €3,000 in and €1,000 out each, plus €2,000 of gifts in the second December. */
    private val database = family.copy(
        transactions = listOf(
            Transaction("a", pedro.id, LocalDate(2025, 11, 1), 3_000_00, "SALARY"),
            Transaction("b", pedro.id, LocalDate(2025, 11, 2), -1_000_00, "SHOP", categoryId = gifts.id),
            Transaction("c", pedro.id, LocalDate(2025, 12, 1), 3_000_00, "SALARY"),
            Transaction("d", pedro.id, LocalDate(2025, 12, 2), -3_000_00, "SHOP", categoryId = gifts.id),
        ),
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun futureFollowsScopeHorizonAndWhatIfs() = runTest {
        val vm =
            FutureViewModel(ObserveDatabaseUseCase(FakeDatabaseRepository.ready(database)), BuildFutureUseCase(), BuildForecastUseCase())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val first = vm.state.value
        assertEquals(FutureScope.ALL, first.future?.scope)
        assertEquals(FutureUiState.DEFAULT_HORIZON, first.horizon)
        assertTrue(first.peakMonths.isNotEmpty())
        val monthly = first.future!!.monthlyMinor

        vm.onHorizonSelected(10)
        assertEquals(10, vm.state.value.horizon)
        vm.onHorizonSelected(7)
        assertEquals(10, vm.state.value.horizon)

        vm.onToggleExtra()
        assertTrue(vm.state.value.future!!.whatIfs.addExtra)
        assertEquals(monthly + 100_00, vm.state.value.future!!.monthlyMinor)
        vm.onToggleCutDining()
        assertTrue(vm.state.value.future!!.whatIfs.cutDining)
        vm.onToggleSetAside()
        assertTrue(vm.state.value.future!!.whatIfs.setAside)
        vm.onToggleExtra()
        assertFalse(vm.state.value.future!!.whatIfs.addExtra)

        vm.onScopeSelected(FutureScope.KIDS)
        assertEquals(FutureScope.KIDS, vm.state.value.future?.scope)
        assertEquals(3, vm.state.value.future?.kids?.size)
    }

    @Test
    fun futureWaitsForTheDatabase() = runTest {
        val vm = FutureViewModel(ObserveDatabaseUseCase(FakeDatabaseRepository()), BuildFutureUseCase(), BuildForecastUseCase())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertNull(vm.state.value.future)
    }

    @Test
    fun yearAheadSwitchesAccounts() = runTest {
        val vm = YearAheadViewModel(ObserveDatabaseUseCase(FakeDatabaseRepository.ready(database)), BuildForecastUseCase())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(AccountScope.All, vm.state.value.forecast?.scope)
        assertEquals("Pedro", vm.state.value.accountNames[pedro.id])
        vm.onScopeSelected(AccountScope.Single(pedro.id))
        assertEquals(AccountScope.Single(pedro.id), vm.state.value.forecast?.scope)
        assertEquals(100, vm.state.value.forecast?.shareOfFamilyPercent)

        val waiting = YearAheadViewModel(ObserveDatabaseUseCase(FakeDatabaseRepository()), BuildForecastUseCase())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { waiting.state.collect {} }
        assertNull(waiting.state.value.forecast)
    }

    @Test
    fun formatting() {
        assertEquals(30_250_00, FutureFormat.roundTo(30_246_10, 10, 2))
        assertEquals(-570_000_00, FutureFormat.roundTo(-570_029_00, 1_000, 2))
        assertEquals("3", FutureFormat.rate(3.0))
        assertEquals("2.5", FutureFormat.rate(2.5))
        assertEquals(100_000_00, FutureFormat.chartMax(87_000_00, 2))
        assertEquals(4_000_00, FutureFormat.chartMax(3_000_00, 2))
        assertEquals(2_00, FutureFormat.chartMax(0, 2))
        val euro = CurrencyCode.of("EUR")
        assertEquals("€100k", FutureFormat.thousands(100_000_00, euro))
        assertEquals("€800", FutureFormat.thousands(800_00, euro))
        val and: (String, String) -> String = { a, b -> "$a and $b" }
        assertEquals("", FutureFormat.joinWithAnd(emptyList(), and))
        assertEquals("Dec", FutureFormat.joinWithAnd(listOf("Dec"), and))
        assertEquals("Dec, Jul and Sep", FutureFormat.joinWithAnd(listOf("Dec", "Jul", "Sep"), and))
    }

    @Test
    fun previewsAreConsistent() {
        assertEquals(FutureScope.KIDS, previewFutureState(FutureScope.KIDS).future?.scope)
        assertNotNull(previewFutureState(FutureScope.JOINT).future)
        assertEquals(4_850_00, previewYearAheadState().forecast?.monthlyIncomeMinor)
        assertNull(previewYearAheadState(AccountScope.Children).forecast?.monthlyIncomeMinor)
    }
}
