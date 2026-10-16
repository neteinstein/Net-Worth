package com.neteinstein.networth.di

import com.neteinstein.networth.core.designsystem.ui.theme.supportsDynamicColor
import com.neteinstein.networth.feature.dashboard.data.InMemoryAccountRepository
import com.neteinstein.networth.feature.dashboard.domain.repository.AccountRepository
import com.neteinstein.networth.feature.dashboard.domain.usecase.ObserveNetWorthUseCase
import com.neteinstein.networth.feature.dashboard.ui.screens.DashboardViewModel
import com.neteinstein.networth.feature.settings.data.InMemorySettingsRepository
import com.neteinstein.networth.feature.settings.domain.repository.SettingsRepository
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetThemeModeUseCase
import com.neteinstein.networth.feature.settings.ui.screens.SettingsViewModel
import com.neteinstein.networth.ui.AppViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The single place where every dependency is registered — shared by Android, iOS and web.
 * Platform entry points only call [initKoin]; they never declare bindings of their own.
 */
val appModule = module {
    // Data
    // Explicit lambdas: the constructors have default args, which singleOf() would try to inject.
    single<AccountRepository> { InMemoryAccountRepository() }
    single<SettingsRepository> { InMemorySettingsRepository() }

    // Domain
    factoryOf(::ObserveNetWorthUseCase)
    factoryOf(::ObserveAppearanceUseCase)
    factoryOf(::SetThemeModeUseCase)
    factoryOf(::SetDynamicColorUseCase)

    // UI
    viewModelOf(::AppViewModel)
    viewModelOf(::DashboardViewModel)
    viewModel {
        SettingsViewModel(
            observeAppearance = get(),
            setThemeMode = get(),
            setDynamicColor = get(),
            dynamicColorAvailable = supportsDynamicColor,
        )
    }
}
