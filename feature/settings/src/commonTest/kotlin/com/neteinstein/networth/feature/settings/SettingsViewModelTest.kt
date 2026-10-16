package com.neteinstein.networth.feature.settings

import com.neteinstein.networth.feature.settings.data.InMemorySettingsRepository
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetThemeModeUseCase
import com.neteinstein.networth.feature.settings.ui.screens.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val repository = InMemorySettingsRepository()

    private fun viewModel(dynamicColorAvailable: Boolean = true) = SettingsViewModel(
        observeAppearance = ObserveAppearanceUseCase(repository),
        setThemeMode = SetThemeModeUseCase(repository),
        setDynamicColor = SetDynamicColorUseCase(repository),
        dynamicColorAvailable = dynamicColorAvailable,
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun selectingThemeModeUpdatesRepositoryAndState() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onThemeModeSelected(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.observeAppearance().first().themeMode)
        assertEquals(ThemeMode.DARK, vm.state.value.appearance.themeMode)
    }

    @Test
    fun togglingDynamicColorUpdatesState() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onDynamicColorToggled(true)

        assertTrue(vm.state.value.appearance.useDynamicColor)
    }

    @Test
    fun exposesPlatformDynamicColorAvailability() {
        assertEquals(false, viewModel(dynamicColorAvailable = false).state.value.dynamicColorAvailable)
    }
}
