package com.neteinstein.networth.feature.dashboard.ui

import com.neteinstein.networth.feature.dashboard.data.InMemoryAccountRepository
import com.neteinstein.networth.feature.dashboard.data.SampleAccounts
import com.neteinstein.networth.feature.dashboard.domain.model.Money
import com.neteinstein.networth.feature.dashboard.domain.model.NetWorthSummary
import com.neteinstein.networth.feature.dashboard.domain.usecase.ObserveNetWorthUseCase
import com.neteinstein.networth.feature.dashboard.ui.screens.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsLoadingThenEmitsSummary() = runTest {
        val viewModel = DashboardViewModel(ObserveNetWorthUseCase(InMemoryAccountRepository()))
        assertTrue(viewModel.state.value.isLoading)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(NetWorthSummary.from(SampleAccounts.all), state.summary)
    }

    @Test
    fun reportsEmptyWhenThereAreNoAccounts() = runTest {
        val viewModel = DashboardViewModel(ObserveNetWorthUseCase(InMemoryAccountRepository(emptyList())))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        assertTrue(viewModel.state.value.isEmpty)
        assertEquals(Money.zero(), viewModel.state.value.summary.netWorth)
    }
}
