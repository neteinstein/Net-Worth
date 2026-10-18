package com.neteinstein.networth.di

import com.neteinstein.networth.core.database.DatabaseRemote
import com.neteinstein.networth.core.database.DriveDatabaseRemote
import com.neteinstein.networth.core.database.DriveFamilyDatabaseRepository
import com.neteinstein.networth.core.database.platformLocalStore
import com.neteinstein.networth.core.designsystem.ui.theme.supportsDynamicColor
import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.FamilyDatabaseRepository
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.repository.LocalStore
import com.neteinstein.networth.core.domain.repository.SystemAppClock
import com.neteinstein.networth.core.domain.repository.UuidGenerator
import com.neteinstein.networth.core.domain.usecase.AddMemberUseCase
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.RestoreSessionUseCase
import com.neteinstein.networth.core.domain.usecase.SetSeasonalUseCase
import com.neteinstein.networth.core.domain.usecase.SignInUseCase
import com.neteinstein.networth.core.domain.usecase.SignOutUseCase
import com.neteinstein.networth.core.domain.usecase.SyncDatabaseUseCase
import com.neteinstein.networth.core.google.DriveClient
import com.neteinstein.networth.core.google.GoogleAuthPlatform
import com.neteinstein.networth.core.google.GoogleAuthRepository
import com.neteinstein.networth.core.google.googleAuthPlatform
import com.neteinstein.networth.core.google.googleHttpClient
import com.neteinstein.networth.feature.alerts.domain.usecase.BuildAlertsUseCase
import com.neteinstein.networth.feature.alerts.ui.screens.AlertsViewModel
import com.neteinstein.networth.feature.future.domain.usecase.BuildForecastUseCase
import com.neteinstein.networth.feature.future.domain.usecase.BuildFutureUseCase
import com.neteinstein.networth.feature.future.ui.screens.FutureViewModel
import com.neteinstein.networth.feature.future.ui.screens.YearAheadViewModel
import com.neteinstein.networth.feature.importer.domain.parse.StatementParser
import com.neteinstein.networth.feature.importer.domain.usecase.CommitImportUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.ParseStatementUseCase
import com.neteinstein.networth.feature.importer.domain.usecase.SaveReviewUseCase
import com.neteinstein.networth.feature.importer.ui.screens.ImportViewModel
import com.neteinstein.networth.feature.importer.ui.screens.ReviewViewModel
import com.neteinstein.networth.feature.manage.domain.usecase.AddCategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.AddSubcategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteCategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteMerchantRuleUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.MergeCategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.RemoveSubcategoryUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAlertSettingsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAssumptionsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SetAskAccountOnImportUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.UpdateCategoryUseCase
import com.neteinstein.networth.feature.manage.ui.screens.CategoriesViewModel
import com.neteinstein.networth.feature.manage.ui.screens.ManageViewModel
import com.neteinstein.networth.feature.onboarding.domain.usecase.CreateHouseholdUseCase
import com.neteinstein.networth.feature.onboarding.domain.usecase.OpenSharedDatabaseUseCase
import com.neteinstein.networth.feature.onboarding.ui.screens.OnboardingViewModel
import com.neteinstein.networth.feature.overview.domain.usecase.BuildOverviewUseCase
import com.neteinstein.networth.feature.overview.domain.usecase.MarkSeasonalUseCase
import com.neteinstein.networth.feature.overview.ui.screens.OverviewViewModel
import com.neteinstein.networth.feature.settings.data.LocalSettingsRepository
import com.neteinstein.networth.feature.settings.domain.repository.SettingsRepository
import com.neteinstein.networth.feature.settings.domain.usecase.CloseDatabaseUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.RenameHouseholdUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SaveMemberUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetLanguageUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetThemeModeUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.ShareDatabaseUseCase
import com.neteinstein.networth.feature.settings.ui.screens.SettingsViewModel
import com.neteinstein.networth.feature.spending.data.InMemoryActiveFilterRepository
import com.neteinstein.networth.feature.spending.domain.repository.ActiveFilterRepository
import com.neteinstein.networth.feature.spending.domain.usecase.BuildSpendingUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.ObserveActiveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.SaveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.SetActiveFilterUseCase
import com.neteinstein.networth.feature.spending.domain.usecase.UpdateTransactionUseCase
import com.neteinstein.networth.feature.spending.ui.screens.FiltersViewModel
import com.neteinstein.networth.feature.spending.ui.screens.SpendingViewModel
import com.neteinstein.networth.feature.spending.ui.screens.TransactionsViewModel
import com.neteinstein.networth.ui.AppViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Qualifier of the process-wide scope background sync runs in. */
val APP_SCOPE = named("appScope")

/**
 * Bindings that touch the platform (files, Google sign-in UI). Kept apart so `AppModuleTest` can
 * replace them with in-memory fakes on the JVM; still part of [appModule].
 */
val platformBindings = module {
    single<LocalStore> { platformLocalStore() }
    // Created at start so Android can follow the foreground Activity from the first screen.
    single<GoogleAuthPlatform>(createdAtStart = true) { googleAuthPlatform() }
}

/**
 * The single place where every dependency is registered — shared by Android, iOS and web.
 * Platform entry points only call [initKoin]; they never declare bindings of their own.
 */
val appModule = module {
    includes(platformBindings)

    // Infrastructure
    single<AppClock> { SystemAppClock }
    single<IdGenerator> { UuidGenerator }
    single<CoroutineScope>(APP_SCOPE) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { GoogleAuthRepository(get()) }
    single<AuthRepository> { get<GoogleAuthRepository>() }
    single { DriveClient(googleHttpClient(), get<GoogleAuthRepository>()) }
    single<DatabaseRemote> { DriveDatabaseRemote(get()) }
    single<FamilyDatabaseRepository> {
        DriveFamilyDatabaseRepository(remote = get(), auth = get(), store = get(), clock = get(), ids = get(), scope = get(APP_SCOPE))
    }
    // Explicit lambda: LocalSettingsRepository is device-local, not a singleOf() candidate with defaults.
    single<SettingsRepository> { LocalSettingsRepository(get()) }

    // Shared use cases
    factoryOf(::RestoreSessionUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveAuthUseCase)
    factoryOf(::ObserveDatabaseUseCase)
    factoryOf(::EditDatabaseUseCase)
    factoryOf(::SyncDatabaseUseCase)
    factoryOf(::AddMemberUseCase)
    factoryOf(::SetSeasonalUseCase)

    // Onboarding
    factoryOf(::CreateHouseholdUseCase)
    factoryOf(::OpenSharedDatabaseUseCase)

    // Overview
    factoryOf(::BuildOverviewUseCase)
    factoryOf(::MarkSeasonalUseCase)

    // Import + Review & learn
    single { StatementParser() }
    // Explicit lambda: ParseStatementUseCase has a default dispatcher argument.
    factory { ParseStatementUseCase(get()) }
    factoryOf(::CommitImportUseCase)
    factoryOf(::SaveReviewUseCase)

    // Spending + Filters
    single<ActiveFilterRepository> { InMemoryActiveFilterRepository() }
    factoryOf(::BuildSpendingUseCase)
    factoryOf(::ObserveActiveFilterUseCase)
    factoryOf(::SetActiveFilterUseCase)
    factoryOf(::SaveFilterUseCase)
    factoryOf(::UpdateTransactionUseCase)

    // Alerts
    factoryOf(::BuildAlertsUseCase)

    // Future
    factoryOf(::BuildFutureUseCase)
    factoryOf(::BuildForecastUseCase)

    // Manage
    factoryOf(::SaveAccountUseCase)
    factoryOf(::DeleteAccountUseCase)
    factoryOf(::SetAskAccountOnImportUseCase)
    factoryOf(::SaveAlertSettingsUseCase)
    factoryOf(::SaveAssumptionsUseCase)
    factoryOf(::DeleteMerchantRuleUseCase)
    factoryOf(::UpdateCategoryUseCase)
    factoryOf(::AddCategoryUseCase)
    factoryOf(::AddSubcategoryUseCase)
    factoryOf(::RemoveSubcategoryUseCase)
    factoryOf(::MergeCategoryUseCase)
    factoryOf(::DeleteCategoryUseCase)

    // Settings
    factoryOf(::ObserveAppearanceUseCase)
    factoryOf(::SetThemeModeUseCase)
    factoryOf(::SetDynamicColorUseCase)
    factoryOf(::SetLanguageUseCase)
    factoryOf(::ShareDatabaseUseCase)
    factoryOf(::CloseDatabaseUseCase)
    factoryOf(::RenameHouseholdUseCase)
    factoryOf(::SaveMemberUseCase)

    // UI
    viewModelOf(::AppViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::OverviewViewModel)
    viewModelOf(::ImportViewModel)
    viewModelOf(::ReviewViewModel)
    viewModelOf(::SpendingViewModel)
    viewModelOf(::FiltersViewModel)
    viewModelOf(::TransactionsViewModel)
    viewModelOf(::AlertsViewModel)
    viewModelOf(::FutureViewModel)
    viewModelOf(::YearAheadViewModel)
    viewModelOf(::ManageViewModel)
    viewModelOf(::CategoriesViewModel)
    viewModel {
        SettingsViewModel(
            observeAppearance = get(),
            observeAuth = get(),
            observeDatabase = get(),
            setThemeMode = get(),
            setDynamicColor = get(),
            setLanguage = get(),
            shareDatabase = get(),
            syncDatabase = get(),
            closeDatabase = get(),
            renameHousehold = get(),
            saveMember = get(),
            signOut = get(),
            dynamicColorAvailable = supportsDynamicColor,
        )
    }
}
