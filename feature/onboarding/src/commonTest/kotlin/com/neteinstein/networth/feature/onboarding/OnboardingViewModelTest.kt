package com.neteinstein.networth.feature.onboarding

import com.neteinstein.networth.core.domain.repository.AuthCancelledException
import com.neteinstein.networth.core.domain.repository.AuthNotConfiguredException
import com.neteinstein.networth.core.domain.repository.DatabaseError
import com.neteinstein.networth.core.domain.repository.DatabaseException
import com.neteinstein.networth.core.domain.repository.DatabaseFolder
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.DriveAccessNotGrantedException
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.SignInUseCase
import com.neteinstein.networth.core.domain.usecase.SignOutUseCase
import com.neteinstein.networth.core.testing.FakeAuthRepository
import com.neteinstein.networth.core.testing.FakeDatabaseRepository
import com.neteinstein.networth.feature.onboarding.domain.usecase.CreateHouseholdUseCase
import com.neteinstein.networth.feature.onboarding.domain.usecase.OpenSharedDatabaseUseCase
import com.neteinstein.networth.feature.onboarding.domain.usecase.PickDatabaseFolderUseCase
import com.neteinstein.networth.feature.onboarding.ui.screens.OnboardingError
import com.neteinstein.networth.feature.onboarding.ui.screens.OnboardingStep
import com.neteinstein.networth.feature.onboarding.ui.screens.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val auth = FakeAuthRepository()
    private val database = FakeDatabaseRepository()

    private fun TestScope.viewModel() = OnboardingViewModel(
        observeAuth = ObserveAuthUseCase(auth),
        signIn = SignInUseCase(auth, database),
        signOut = SignOutUseCase(auth, database),
        createHousehold = CreateHouseholdUseCase(database),
        openSharedDatabase = OpenSharedDatabaseUseCase(auth, database),
        pickDatabaseFolder = PickDatabaseFolderUseCase(auth, database),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun signInMovesToTheChoice() = runTest {
        val vm = viewModel()
        assertEquals(OnboardingStep.SIGN_IN, vm.state.value.step)

        vm.onSignIn()

        assertEquals(OnboardingStep.CHOOSE, vm.state.value.step)
        assertEquals("pedro@example.com", vm.state.value.user?.email)
        assertEquals(listOf("restore"), database.calls)
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun signInFailuresBecomeMessages() = runTest {
        val vm = viewModel()

        auth.signInResult = { throw AuthNotConfiguredException() }
        vm.onSignIn()
        assertEquals(OnboardingError.NOT_CONFIGURED, vm.state.value.error)

        vm.onErrorShown()
        auth.signInResult = { throw IllegalStateException("network") }
        vm.onSignIn()
        assertEquals(OnboardingError.SIGN_IN_FAILED, vm.state.value.error)

        vm.onErrorShown()
        auth.signInResult = { throw DriveAccessNotGrantedException() }
        vm.onSignIn()
        assertEquals(OnboardingError.NO_DRIVE_PERMISSION, vm.state.value.error)
        assertEquals(OnboardingStep.SIGN_IN, vm.state.value.step)

        vm.onErrorShown()
        auth.signInResult = { throw AuthCancelledException() }
        vm.onSignIn()
        assertNull(vm.state.value.error)
    }

    @Test
    fun startFreshPrefillsTheFirstAdultAndCreatesTheDatabase() = runTest {
        auth.signIn()
        val vm = viewModel()

        vm.onStartFresh()
        assertEquals(OnboardingStep.START_FRESH, vm.state.value.step)
        assertEquals(listOf("Pedro"), vm.state.value.form.adults)
        assertFalse(vm.state.value.form.canSubmit)

        vm.onHouseholdNameChange("The Vicentes")
        vm.onAddAdult()
        vm.onAdultChange(1, "Maria")
        vm.onAddChild()
        vm.onChildChange(0, "Inês")
        vm.onAddChild()
        vm.onRemoveChild(1)
        vm.onCurrencySelected("GBP")
        vm.onCreateAccountsChange(false)
        vm.onCreate(jointSavingsName = "Poupança conjunta")

        assertEquals(listOf("create:The Vicentes"), database.calls)
        val created = assertIs<DatabaseState.Ready>(database.state.value).database
        assertEquals(listOf("Pedro", "Maria", "Inês"), created.members.map { it.name })
        assertEquals("GBP", created.settings.currencyCode)
        assertTrue(created.accounts.isEmpty())
        assertEquals("Net Worth – The Vicentes.networth.json", assertIs<DatabaseState.Ready>(database.state.value).file.name)
    }

    @Test
    fun theAppChoosesTheFileUnlessTheUserWantsTo() = runTest {
        auth.signIn()
        val vm = viewModel()
        vm.onStartFresh()
        vm.onHouseholdNameChange("The Vicentes")
        assertFalse(vm.state.value.form.chooseFile)
        assertEquals("Net Worth – The Vicentes.networth.json", vm.state.value.form.resolvedFileName)

        // Typed details only count once the user says they want to choose.
        vm.onFileNameChange("Our money")
        assertEquals("Net Worth – The Vicentes.networth.json", vm.state.value.form.resolvedFileName)
        vm.onChooseFileChange(true)
        assertEquals("Our money.networth.json", vm.state.value.form.resolvedFileName)

        vm.onPickFolder()
        assertEquals(DatabaseFolder("picked-folder", "Folder picked-folder"), vm.state.value.form.folder)
        vm.onCreate("Joint savings")

        assertEquals(listOf("create:The Vicentes name:Our money in:picked-folder"), database.calls)
    }

    @Test
    fun choosingCanFallBackToTheAppsChoice() = runTest {
        auth.signIn()
        val vm = viewModel()
        vm.onStartFresh()
        vm.onHouseholdNameChange("The Vicentes")
        vm.onChooseFileChange(true)
        vm.onPickFolder()

        auth.pickFolderResult = { null }
        vm.onPickFolder()
        assertEquals("picked-folder", vm.state.value.form.folder?.id)

        vm.onUseMyDrive()
        assertNull(vm.state.value.form.folder)
        // A blank name keeps the app's name.
        assertEquals("Net Worth – The Vicentes.networth.json", vm.state.value.form.resolvedFileName)
        vm.onCreate("Joint savings")
        assertEquals(listOf("create:The Vicentes"), database.calls)

        auth.pickFolderResult = { "unreadable" }
        database.failure = DatabaseException(DatabaseError.NOT_ACCESSIBLE)
        vm.onPickFolder()
        assertEquals(OnboardingError.NOT_ACCESSIBLE, vm.state.value.error)
    }

    @Test
    fun switchingBackToTheAppsChoiceIgnoresTypedDetails() = runTest {
        auth.signIn()
        val vm = viewModel()
        vm.onStartFresh()
        vm.onHouseholdNameChange("The Vicentes")
        vm.onChooseFileChange(true)
        vm.onFileNameChange("Our money")
        vm.onPickFolder()

        vm.onChooseFileChange(false)
        vm.onCreate("Joint savings")

        assertEquals(listOf("create:The Vicentes"), database.calls)
    }

    @Test
    fun formEditsKeepAtLeastOneAdult() = runTest {
        auth.signIn()
        val vm = viewModel()
        vm.onStartFresh()

        vm.onRemoveAdult(0)
        vm.onCreate("Joint savings")

        assertEquals(1, vm.state.value.form.adults.size)
        assertTrue(database.calls.isEmpty())

        vm.onBackToChoices()
        assertEquals(OnboardingStep.CHOOSE, vm.state.value.step)
    }

    @Test
    fun openExistingUsesThePickerAndReportsUnreadableFiles() = runTest {
        auth.signIn()
        val vm = viewModel()

        vm.onOpenExisting()
        assertEquals(listOf("open:picked-file"), database.calls)

        auth.pickResult = { null }
        vm.onOpenExisting()
        assertEquals(1, database.calls.size)

        auth.pickResult = { "x" }
        for ((error, expected) in listOf(
            DatabaseError.NOT_A_DATABASE to OnboardingError.NOT_A_DATABASE,
            DatabaseError.NEWER_SCHEMA to OnboardingError.NEWER_SCHEMA,
            DatabaseError.NOT_ACCESSIBLE to OnboardingError.NOT_ACCESSIBLE,
            DatabaseError.NO_DRIVE_PERMISSION to OnboardingError.NO_DRIVE_PERMISSION,
            DatabaseError.DRIVE_API_DISABLED to OnboardingError.DRIVE_API_DISABLED,
            DatabaseError.DRIVE_FULL to OnboardingError.DRIVE_FULL,
            DatabaseError.UNKNOWN to OnboardingError.NETWORK,
        )) {
            database.failure = DatabaseException(error)
            vm.onOpenExisting()
            assertEquals(expected, vm.state.value.error)
            vm.onErrorShown()
        }
        database.failure = IllegalStateException("drive down")
        vm.onOpenExisting()
        assertEquals(OnboardingError.NETWORK, vm.state.value.error)
    }

    @Test
    fun useAnotherAccountSignsOut() = runTest {
        auth.signIn()
        val vm = viewModel()
        vm.onStartFresh()

        vm.onSignOut()

        assertEquals(OnboardingStep.SIGN_IN, vm.state.value.step)
        assertEquals(listOf("close"), database.calls)
    }
}
