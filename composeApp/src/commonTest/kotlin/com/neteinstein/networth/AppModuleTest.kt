package com.neteinstein.networth

import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.FamilyDatabaseRepository
import com.neteinstein.networth.core.domain.repository.InMemoryLocalStore
import com.neteinstein.networth.core.domain.repository.LocalStore
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.core.domain.usecase.AddMemberUseCase
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.RestoreSessionUseCase
import com.neteinstein.networth.core.domain.usecase.SetSeasonalUseCase
import com.neteinstein.networth.core.domain.usecase.SignOutUseCase
import com.neteinstein.networth.core.google.DriveClient
import com.neteinstein.networth.core.google.GoogleAuthPlatform
import com.neteinstein.networth.di.appModule
import com.neteinstein.networth.feature.alerts.domain.usecase.BuildAlertsUseCase
import com.neteinstein.networth.feature.future.domain.usecase.BuildForecastUseCase
import com.neteinstein.networth.feature.future.domain.usecase.BuildFutureUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.CommitImportUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.ParseStatementUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.SaveReviewUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.MergeCategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAssumptionsUseCase
import com.neteinstein.networth.feature.onboarding.domain.usecase.CreateHouseholdUseCase
import com.neteinstein.networth.feature.onboarding.domain.usecase.OpenSharedDatabaseUseCase
import com.neteinstein.networth.feature.overview.domain.usecase.BuildOverviewUseCase
import com.neteinstein.networth.feature.overview.domain.usecase.MarkSeasonalUseCase
import com.neteinstein.networth.feature.settings.domain.repository.SettingsRepository
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetLanguageUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetThemeModeUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.ShareDatabaseUseCase
import com.neteinstein.networth.feature.spending.domain.repository.ActiveFilterRepository
import com.neteinstein.networth.feature.spending.domain.usecase.BuildSpendingUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.SaveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.UpdateTransactionUseCase
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/** Fails fast if a binding in `AppModule.kt` is missing or mis-typed. */
class AppModuleTest {
    // The JVM test host has no Android Context or browser, so platform bindings are replaced.
    private val fakes = module {
        single<LocalStore> { InMemoryLocalStore() }
        single<GoogleAuthPlatform> {
            object : GoogleAuthPlatform {
                override val isConfigured = false

                override suspend fun restore(): UserProfile? = null

                override suspend fun signIn(): UserProfile = error("not in tests")

                override suspend fun accessToken(forceRefresh: Boolean): String = error("not in tests")

                override suspend fun pickFile(): String? = null

                override suspend fun signOut() = Unit
            }
        }
    }

    @Test
    fun resolvesNonViewModelBindings() {
        val koin = koinApplication {
            allowOverride(true)
            modules(appModule, fakes)
        }.koin

        assertNotNull(koin.get<RestoreSessionUseCase>())
        assertNotNull(koin.get<SignOutUseCase>())
        assertNotNull(koin.get<EditDatabaseUseCase>())
        assertNotNull(koin.get<CreateHouseholdUseCase>())
        assertNotNull(koin.get<OpenSharedDatabaseUseCase>())
        assertNotNull(koin.get<BuildOverviewUseCase>())
        assertNotNull(koin.get<MarkSeasonalUseCase>())
        assertNotNull(koin.get<SaveAccountUseCase>())
        assertNotNull(koin.get<MergeCategoryUseCase>())
        assertNotNull(koin.get<ObserveAppearanceUseCase>())
        assertNotNull(koin.get<SetThemeModeUseCase>())
        assertNotNull(koin.get<SetDynamicColorUseCase>())
        assertNotNull(koin.get<SetLanguageUseCase>())
        assertNotNull(koin.get<ShareDatabaseUseCase>())
        assertNotNull(koin.get<DriveClient>())
        assertNotNull(koin.get<AddMemberUseCase>())
        assertNotNull(koin.get<ParseStatementUseCase>())
        assertNotNull(koin.get<CommitImportUseCase>())
        assertNotNull(koin.get<SaveReviewUseCase>())
        assertNotNull(koin.get<SetSeasonalUseCase>())
        assertNotNull(koin.get<BuildSpendingUseCase>())
        assertNotNull(koin.get<SaveFilterUseCase>())
        assertNotNull(koin.get<UpdateTransactionUseCase>())
        assertNotNull(koin.get<BuildAlertsUseCase>())
        assertNotNull(koin.get<BuildFutureUseCase>())
        assertNotNull(koin.get<SaveAssumptionsUseCase>())
        assertNotNull(koin.get<BuildForecastUseCase>())
        assertSame(koin.get<ActiveFilterRepository>(), koin.get<ActiveFilterRepository>())
        assertSame(koin.get<AuthRepository>(), koin.get<AuthRepository>())
        assertSame(koin.get<FamilyDatabaseRepository>(), koin.get<FamilyDatabaseRepository>())
        assertSame(koin.get<SettingsRepository>(), koin.get<SettingsRepository>())
    }
}
