package com.neteinstein.networth

import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.InMemoryLocalStore
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.RestoreSessionUseCase
import com.neteinstein.networth.core.testing.FakeAuthRepository
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.core.testing.testUser
import com.neteinstein.networth.feature.settings.data.LocalSettingsRepository
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import com.neteinstein.networth.ui.AppViewModel
import com.neteinstein.networth.ui.RootDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val settings = LocalSettingsRepository(InMemoryLocalStore())
    private val auth = FakeAuthRepository()
    private val database = FakeDatabaseRepository(DatabaseState.Loading)

    private fun viewModel() = AppViewModel(
        ObserveAppearanceUseCase(settings),
        ObserveAuthUseCase(auth),
        ObserveDatabaseUseCase(database),
        RestoreSessionUseCase(auth, database),
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun followsAppearanceSettings() = runTest {
        val viewModel = viewModel()
        assertEquals(ThemeMode.SYSTEM, viewModel.appearance.value.themeMode)

        settings.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.appearance.value.themeMode)
    }

    @Test
    fun routesBetweenLoadingOnboardingAndTheApp() = runTest {
        auth.state.value = AuthState.Unknown
        val viewModel = viewModel()
        // The fake's restore() keeps whatever state it has.
        assertEquals(RootDestination.LOADING, viewModel.destination.value)

        auth.state.value = AuthState.SignedOut
        assertEquals(RootDestination.ONBOARDING, viewModel.destination.value)

        auth.state.value = AuthState.SignedIn(testUser)
        assertEquals(RootDestination.LOADING, viewModel.destination.value)

        database.state.value = DatabaseState.NotSelected
        assertEquals(RootDestination.ONBOARDING, viewModel.destination.value)

        database.state.value = FakeDatabaseRepository.ready(designFamily()).state.value
        assertEquals(RootDestination.MAIN, viewModel.destination.value)
    }

    @Test
    fun restoresTheSessionOnStart() = runTest {
        auth.state.value = AuthState.SignedIn(testUser)

        viewModel()

        assertEquals(listOf("restore"), database.calls)
    }
}
