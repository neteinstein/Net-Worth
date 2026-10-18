package com.neteinstein.networth.feature.settings.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.DatabaseFile
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.core.domain.usecase.ObserveAuthUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.core.domain.usecase.SignOutUseCase
import com.neteinstein.networth.core.domain.usecase.SyncDatabaseUseCase
import com.neteinstein.networth.feature.settings.domain.model.AppLanguage
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.domain.usecase.CloseDatabaseUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.ObserveAppearanceUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.RenameHouseholdUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SaveMemberUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetLanguageUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.SetThemeModeUseCase
import com.neteinstein.networth.feature.settings.domain.usecase.ShareDatabaseUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ShareResult { SHARED, FAILED }

data class SettingsUiState(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val dynamicColorAvailable: Boolean = false,
    val email: String? = null,
    val file: DatabaseFile? = null,
    val sync: SyncStatus? = null,
    val householdName: String = "",
    val members: List<Member> = emptyList(),
    val sharing: Boolean = false,
    val shareResult: ShareResult? = null,
)

class SettingsViewModel(
    observeAppearance: ObserveAppearanceUseCase,
    observeAuth: ObserveAuthUseCase,
    observeDatabase: ObserveDatabaseUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setDynamicColor: SetDynamicColorUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val shareDatabase: ShareDatabaseUseCase,
    private val syncDatabase: SyncDatabaseUseCase,
    private val closeDatabase: CloseDatabaseUseCase,
    private val renameHousehold: RenameHouseholdUseCase,
    private val saveMember: SaveMemberUseCase,
    private val signOut: SignOutUseCase,
    dynamicColorAvailable: Boolean,
) : ViewModel() {
    private val sharing = MutableStateFlow(false to null as ShareResult?)

    val state: StateFlow<SettingsUiState> = combine(observeAppearance(), observeAuth(), observeDatabase(), sharing) {
        appearance,
        auth,
        db,
        share,
        ->
        val ready = db as? DatabaseState.Ready
        SettingsUiState(
            appearance = appearance,
            dynamicColorAvailable = dynamicColorAvailable,
            email = (auth as? AuthState.SignedIn)?.user?.email,
            file = ready?.file,
            sync = ready?.sync,
            householdName = ready?.database?.settings?.householdName.orEmpty(),
            members = ready?.database?.members.orEmpty(),
            sharing = share.first,
            shareResult = share.second,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(dynamicColorAvailable = dynamicColorAvailable),
    )

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun onDynamicColorToggled(enabled: Boolean) {
        viewModelScope.launch { setDynamicColor(enabled) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch { setLanguage(language) }
    }

    fun onShare(email: String) {
        if (!ShareDatabaseUseCase.isEmail(email) || sharing.value.first) return
        viewModelScope.launch {
            sharing.value = true to null
            val result = try {
                shareDatabase(email)
                ShareResult.SHARED
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ShareResult.FAILED
            }
            sharing.value = false to result
        }
    }

    fun onShareResultShown() {
        sharing.value = sharing.value.first to null
    }

    fun onSyncNow() {
        viewModelScope.launch { syncDatabase() }
    }

    fun onSwitchDatabase() {
        viewModelScope.launch { closeDatabase() }
    }

    fun onRenameHousehold(name: String) {
        viewModelScope.launch { renameHousehold(name) }
    }

    fun onSaveMember(id: String?, name: String, role: MemberRole) {
        viewModelScope.launch { saveMember(id, name, role) }
    }

    fun onSignOut() {
        viewModelScope.launch { signOut() }
    }
}
