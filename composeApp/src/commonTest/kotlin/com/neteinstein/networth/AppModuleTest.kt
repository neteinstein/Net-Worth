package com.neteinstein.networth

import com.neteinstein.networth.di.appModule
import com.neteinstein.networth.feature.dashboard.domain.repository.AccountRepository
import com.neteinstein.networth.feature.dashboard.domain.usecase.ObserveNetWorthUseCase
import com.neteinstein.networth.feature.settings.domain.repository.SettingsRepository
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetThemeModeUseCase
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/** Fails fast if a binding in `AppModule.kt` is missing or mis-typed. */
class AppModuleTest {
    @Test
    fun resolvesNonViewModelBindings() {
        val koin = koinApplication { modules(appModule) }.koin

        assertNotNull(koin.get<ObserveNetWorthUseCase>())
        assertNotNull(koin.get<ObserveAppearanceUseCase>())
        assertNotNull(koin.get<SetThemeModeUseCase>())
        assertNotNull(koin.get<SetDynamicColorUseCase>())
        assertSame(koin.get<AccountRepository>(), koin.get<AccountRepository>())
        assertSame(koin.get<SettingsRepository>(), koin.get<SettingsRepository>())
    }
}
