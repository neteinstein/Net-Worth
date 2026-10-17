package com.neteinstein.networth.feature.settings

import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.InMemoryLocalStore
import com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.SignOutUseCase
import com.neteinstein.networth.core.domain.usecase.SyncDatabaseUseCase
import com.neteinstein.networth.core.testing.FakeAuthRepository
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.core.testing.SequentialIds
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.settings.data.LocalSettingsRepository
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.domain.usecase.CloseDatabaseUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.RenameHouseholdUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SaveMemberUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetThemeModeUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.ShareDatabaseUseCase
import com.neteinstein.networth.feature.settings.ui.screens.SettingsViewModel
import com.neteinstein.networth.feature.settings.ui.screens.ShareResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val store = InMemoryLocalStore()
    private val settings = LocalSettingsRepository(store)
    private val auth = FakeAuthRepository(signedIn = true)
    private val database = FakeDatabaseRepository.ready(designFamily())
    private val edit = EditDatabaseUseCase(database)

    private fun TestScope.viewModel(dynamicColorAvailable: Boolean = true) = SettingsViewModel(
        observeAppearance = ObserveAppearanceUseCase(settings),
        observeAuth = ObserveAuthUseCase(auth),
        observeDatabase = ObserveDatabaseUseCase(database),
        setThemeMode = SetThemeModeUseCase(settings),
        setDynamicColor = SetDynamicColorUseCase(settings),
        shareDatabase = ShareDatabaseUseCase(database),
        syncDatabase = SyncDatabaseUseCase(database),
        closeDatabase = CloseDatabaseUseCase(database),
        renameHousehold = RenameHouseholdUseCase(edit),
        saveMember = SaveMemberUseCase(edit, SequentialIds("m")),
        signOut = SignOutUseCase(auth, database),
        dynamicColorAvailable = dynamicColorAvailable,
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun appearanceIsPersistedOnThisDevice() = runTest {
        val vm = viewModel()

        vm.onThemeModeSelected(ThemeMode.DARK)
        vm.onDynamicColorToggled(true)

        assertEquals(ThemeMode.DARK, vm.state.value.appearance.themeMode)
        assertTrue(vm.state.value.appearance.useDynamicColor)
        val reloaded = LocalSettingsRepository(store).observeAppearance().first()
        assertEquals(ThemeMode.DARK, reloaded.themeMode)
        assertTrue(reloaded.useDynamicColor)
        assertEquals(
            ThemeMode.SYSTEM,
            LocalSettingsRepository(InMemoryLocalStore(mapOf("appearance.txt" to "junk"))).observeAppearance().first().themeMode,
        )
    }

    @Test
    fun exposesAccountFileAndHousehold() = runTest {
        val state = viewModel(dynamicColorAvailable = false).state.value

        assertEquals("pedro@example.com", state.email)
        assertEquals("file-1", state.file?.id)
        assertEquals("Vicente", state.householdName)
        assertEquals(5, state.members.size)
        assertFalse(state.dynamicColorAvailable)
    }

    @Test
    fun sharesWithValidEmailsAndReportsFailures() = runTest {
        val vm = viewModel()

        vm.onShare("not an email")
        assertTrue(database.calls.isEmpty())

        vm.onShare(" maria@example.com ")
        assertEquals(listOf("share:maria@example.com"), database.calls)
        assertEquals(ShareResult.SHARED, vm.state.value.shareResult)
        vm.onShareResultShown()
        assertNull(vm.state.value.shareResult)

        database.failure = IllegalStateException("offline")
        vm.onShare("maria@example.com")
        assertEquals(ShareResult.FAILED, vm.state.value.shareResult)
        assertFalse(vm.state.value.sharing)
    }

    @Test
    fun editsHouseholdAndMembers() = runTest {
        val vm = viewModel()
        val pedro = database.database.members.first()

        vm.onRenameHousehold("Os Vicentes")
        vm.onRenameHousehold(" ")
        vm.onSaveMember(pedro.id, "Pedro M.", MemberRole.ADULT)
        vm.onSaveMember(null, "Baby", MemberRole.CHILD)
        vm.onSaveMember(null, "", MemberRole.CHILD)

        assertEquals("Os Vicentes", database.database.settings.householdName)
        assertEquals("Pedro M.", database.database.member(pedro.id)?.name)
        assertEquals("Baby", database.database.members.last().name)
        assertEquals(6, database.database.members.size)
    }

    @Test
    fun syncSwitchAndSignOut() = runTest {
        val vm = viewModel()

        vm.onSyncNow()
        vm.onSwitchDatabase()
        assertEquals(DatabaseState.NotSelected, database.state.value)
        vm.onSignOut()

        assertEquals(listOf("sync", "close", "close"), database.calls)
        assertEquals(AuthState.SignedOut, auth.state.value)
    }
}
