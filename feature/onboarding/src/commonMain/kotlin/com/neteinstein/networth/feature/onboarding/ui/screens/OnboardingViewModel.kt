package com.neteinstein.networth.feature.onboarding.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.repository.AuthCancelledException
import com.neteinstein.networth.core.domain.repository.AuthNotConfiguredException
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseError
import com.neteinstein.networth.core.domain.repository.DatabaseException
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.SignInUseCase
import com.neteinstein.networth.core.domain.usecase.SignOutUseCase
import com.neteinstein.networth.feature.onboarding.domain.model.HouseholdForm
import com.neteinstein.networth.feature.onboarding.domain.usecase.CreateHouseholdUseCase
import com.neteinstein.networth.feature.onboarding.domain.usecase.OpenSharedDatabaseUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { SIGN_IN, CHOOSE, START_FRESH }

enum class OnboardingError { NOT_CONFIGURED, SIGN_IN_FAILED, NOT_A_DATABASE, NEWER_SCHEMA, NOT_ACCESSIBLE, NETWORK }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.SIGN_IN,
    val user: UserProfile? = null,
    val form: HouseholdForm = HouseholdForm(),
    val busy: Boolean = false,
    val error: OnboardingError? = null,
)

/**
 * First run: sign in with Google, then "Start fresh" (new database file in Drive) or "Open a Google
 * Drive file" (Picker). Once a database is open the app shell leaves onboarding by itself.
 */
class OnboardingViewModel(
    observeAuth: ObserveAuthUseCase,
    private val signIn: SignInUseCase,
    private val signOut: SignOutUseCase,
    private val createHousehold: CreateHouseholdUseCase,
    private val openSharedDatabase: OpenSharedDatabaseUseCase,
) : ViewModel() {
    private val startingFresh = MutableStateFlow(false)
    private val form = MutableStateFlow(HouseholdForm())
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<OnboardingError?>(null)
    private val auth = observeAuth()

    val state: StateFlow<OnboardingUiState> = combine(auth, startingFresh, form, busy, error) { auth, fresh, form, busy, error ->
        val user = (auth as? AuthState.SignedIn)?.user
        OnboardingUiState(
            step = when {
                user == null -> OnboardingStep.SIGN_IN
                fresh -> OnboardingStep.START_FRESH
                else -> OnboardingStep.CHOOSE
            },
            user = user,
            form = form,
            busy = busy,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    fun onSignIn() = run {
        try {
            signIn()
        } catch (e: AuthNotConfiguredException) {
            error.value = OnboardingError.NOT_CONFIGURED
        }
    }

    fun onStartFresh() {
        val firstName = (auth.value as? AuthState.SignedIn)?.user?.displayName?.substringBefore(' ')
        form.update { if (it.adults.all(String::isBlank) && firstName != null) it.copy(adults = listOf(firstName)) else it }
        startingFresh.value = true
    }

    fun onBackToChoices() {
        startingFresh.value = false
    }

    fun onOpenExisting() = run { openSharedDatabase() }

    fun onCreate(jointSavingsName: String) {
        val current = form.value
        if (!current.canSubmit) return
        run { createHousehold(current.toSetup(jointSavingsName)) }
    }

    fun onSignOut() = run {
        signOut()
        startingFresh.value = false
    }

    fun onErrorShown() {
        error.value = null
    }

    fun onHouseholdNameChange(name: String) = form.update { it.copy(householdName = name) }

    fun onAdultChange(index: Int, name: String) = form.update { it.copy(adults = it.adults.replace(index, name)) }

    fun onAddAdult() = form.update { it.copy(adults = it.adults + "") }

    fun onRemoveAdult(index: Int) = form.update { if (it.adults.size > 1) it.copy(adults = it.adults.without(index)) else it }

    fun onChildChange(index: Int, name: String) = form.update { it.copy(children = it.children.replace(index, name)) }

    fun onAddChild() = form.update { it.copy(children = it.children + "") }

    fun onRemoveChild(index: Int) = form.update { it.copy(children = it.children.without(index)) }

    fun onCurrencySelected(code: String) = form.update { it.copy(currencyCode = code) }

    fun onCreateAccountsChange(create: Boolean) = form.update { it.copy(createAccounts = create) }

    /** Runs one action at a time, mapping failures to a message the screen can show. */
    private fun run(action: suspend () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: AuthCancelledException) {
                // The user backed out of a Google screen: nothing to report.
            } catch (e: DatabaseException) {
                error.value = when (e.error) {
                    DatabaseError.NOT_A_DATABASE -> OnboardingError.NOT_A_DATABASE
                    DatabaseError.NEWER_SCHEMA -> OnboardingError.NEWER_SCHEMA
                    DatabaseError.NOT_ACCESSIBLE -> OnboardingError.NOT_ACCESSIBLE
                    DatabaseError.UNKNOWN -> OnboardingError.NETWORK
                }
            } catch (_: Exception) {
                error.value = if (auth.value !is AuthState.SignedIn) OnboardingError.SIGN_IN_FAILED else OnboardingError.NETWORK
            } finally {
                busy.value = false
            }
        }
    }

    private fun List<String>.replace(index: Int, value: String) = mapIndexed { i, old -> if (i == index) value else old }

    private fun List<String>.without(index: Int) = filterIndexed { i, _ -> i != index }
}
