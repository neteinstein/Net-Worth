package com.neteinstein.networth.feature.manage.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.domain.calc.balanceOf
import com.neteinstein.networth.core.domain.calc.isChildAccount
import com.neteinstein.networth.core.domain.format.MoneyParser
import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.AlertSettings
import com.neteinstein.networth.core.domain.model.Assumptions
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.SavedFilter
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.AddMemberUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.AccountDraft
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteMerchantRuleUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAlertSettingsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAssumptionsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SetAskAccountOnImportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManagedAccount(
    val account: Account,
    val balanceMinor: Long,
    val isChild: Boolean,
    val hasTransactions: Boolean,
)

data class RuleLine(
    val id: String,
    val pattern: String,
    val categoryName: String,
    val colorHex: String,
)

/** The account dialog's fields, kept as typed text until saved. */
data class AccountEditor(
    val id: String? = null,
    val name: String = "",
    val type: AccountType = AccountType.CURRENT,
    val ownerIds: List<String> = emptyList(),
    val bank: String = "",
    val accountNumber: String = "",
    val iban: String = "",
    val balance: String = "",
    val canDelete: Boolean = false,
) {
    val balanceIsValid: Boolean get() = balance.isBlank() || MoneyParser.parseMinor(balance) != null
    val canSave: Boolean get() = name.isNotBlank() && balanceIsValid
}

data class ManageUiState(
    val loading: Boolean = true,
    val currencyCode: String = "EUR",
    val accounts: List<ManagedAccount> = emptyList(),
    val members: List<Member> = emptyList(),
    val askAccountOnImport: Boolean = true,
    val categoryCount: Int = 0,
    val subcategoryCount: Int = 0,
    val categoryColors: List<String> = emptyList(),
    val rules: List<RuleLine> = emptyList(),
    val savedFilters: List<SavedFilter> = emptyList(),
    val alerts: AlertSettings = AlertSettings(),
    val assumptions: Assumptions = Assumptions(),
    val editor: AccountEditor? = null,
)

class ManageViewModel(
    observeDatabase: ObserveDatabaseUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val setAskAccount: SetAskAccountOnImportUseCase,
    private val saveAlertSettings: SaveAlertSettingsUseCase,
    private val saveAssumptions: SaveAssumptionsUseCase,
    private val deleteRule: DeleteMerchantRuleUseCase,
    private val addMember: AddMemberUseCase,
) : ViewModel() {
    private val editor = MutableStateFlow<AccountEditor?>(null)

    val state: StateFlow<ManageUiState> = combine(observeDatabase(), editor) { db, editor ->
        val database = (db as? DatabaseState.Ready)?.database ?: return@combine ManageUiState(editor = editor)
        val categories = database.categories.sortedBy { it.position }
        ManageUiState(
            loading = false,
            currencyCode = database.settings.currencyCode,
            accounts = database.accounts.map { account ->
                ManagedAccount(
                    account,
                    database.balanceOf(account),
                    database.isChildAccount(account),
                    database.transactions.any {
                        it.accountId ==
                            account.id
                    },
                )
            },
            members = database.members,
            askAccountOnImport = database.settings.askAccountOnImport,
            categoryCount = categories.size,
            subcategoryCount = categories.sumOf { it.subcategories.size },
            categoryColors = categories.take(4).map { it.colorHex },
            rules = database.merchantRules.sortedByDescending { it.hits }.mapNotNull { rule ->
                val category = database.category(rule.categoryId) ?: return@mapNotNull null
                RuleLine(rule.id, rule.pattern, category.name, category.colorHex)
            },
            savedFilters = database.savedFilters,
            alerts = database.settings.alerts,
            assumptions = database.settings.assumptions,
            editor = editor,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageUiState())

    fun onAddAccount() {
        editor.value = AccountEditor()
    }

    fun onEditAccount(accountId: String) {
        val managed = state.value.accounts.firstOrNull { it.account.id == accountId } ?: return
        val account = managed.account
        editor.value = AccountEditor(
            id = account.id,
            name = account.name,
            type = account.type,
            ownerIds = account.ownerIds,
            bank = account.bank.orEmpty(),
            accountNumber = account.accountNumber.orEmpty(),
            iban = account.iban.orEmpty(),
            canDelete = !managed.hasTransactions,
        )
    }

    fun onEditorChange(change: (AccountEditor) -> AccountEditor) = editor.update { it?.let(change) }

    /** Adds someone to the household from the owner picker and makes them an owner of the edited account. */
    fun onAddPerson(name: String, role: MemberRole) {
        viewModelScope.launch {
            val member = addMember(name, role)
            editor.update { it?.copy(ownerIds = it.ownerIds + member.id) }
        }
    }

    fun onDismissEditor() {
        editor.value = null
    }

    fun onSaveAccount() {
        val current = editor.value?.takeIf { it.canSave } ?: return
        editor.value = null
        viewModelScope.launch {
            saveAccount(
                AccountDraft(
                    id = current.id,
                    name = current.name,
                    type = current.type,
                    ownerIds = current.ownerIds,
                    bank = current.bank,
                    accountNumber = current.accountNumber,
                    iban = current.iban,
                    balanceMinor = current.balance.takeIf { it.isNotBlank() }?.let(MoneyParser::parseMinor),
                ),
            )
        }
    }

    fun onDeleteAccount() {
        val id = editor.value?.takeIf { it.canDelete }?.id ?: return
        editor.value = null
        viewModelScope.launch { deleteAccount(id) }
    }

    fun onAskAccountChange(ask: Boolean) {
        viewModelScope.launch { setAskAccount(ask) }
    }

    fun onSaveAlerts(spikeThresholdPct: Int, largePaymentMinor: Long, newSubscriptions: Boolean) {
        viewModelScope.launch { saveAlertSettings(spikeThresholdPct, largePaymentMinor, newSubscriptions) }
    }

    fun onSaveAssumptions(assumptions: Assumptions) {
        viewModelScope.launch { saveAssumptions(assumptions) }
    }

    fun onDeleteRule(ruleId: String) {
        viewModelScope.launch { deleteRule(ruleId) }
    }
}
