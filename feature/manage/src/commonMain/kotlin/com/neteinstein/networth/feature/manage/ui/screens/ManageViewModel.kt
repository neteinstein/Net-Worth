package com.neteinstein.networth.feature.manage.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.networth.core.designsystem.ui.files.PickedFile
import com.neteinstein.networth.core.domain.calc.balanceOf
import com.neteinstein.networth.core.domain.calc.isChildAccount
import com.neteinstein.networth.core.domain.format.MoneyParser
import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.AlertSettings
import com.neteinstein.networth.core.domain.model.Assumptions
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.RuleMatch
import com.neteinstein.networth.core.domain.model.SavedFilter
import com.neteinstein.networth.core.domain.repository.DatabaseState
import com.neteinstein.networth.core.domain.usecase.AddMemberUseCase
import com.neteinstein.networth.core.domain.usecase.ObserveDatabaseUseCase
import com.neteinstein.networth.feature.manage.domain.RuleFile
import com.neteinstein.networth.feature.manage.domain.RuleFileContent
import com.neteinstein.networth.feature.manage.domain.RuleFileError
import com.neteinstein.networth.feature.manage.domain.usecase.AccountDraft
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.DeleteMerchantRuleUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.ImportMerchantRulesUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAccountUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAlertSettingsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveAssumptionsUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SaveMerchantRuleUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.SetAskAccountOnImportUseCase
import com.neteinstein.networth.feature.manage.domain.usecase.refiledBy
import com.neteinstein.networth.feature.manage.domain.usecase.transactionsMatching
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

/** The merchant rule dialog: the rule as edited, and how many past transactions it matches. */
data class RuleEditor(
    val rule: MerchantRule,
    val pastMatches: Int = 0,
    /** Of [pastMatches], those filed somewhere else today, i.e. what "apply to past" would change. */
    val pastToRefile: Int = 0,
) {
    val canSave: Boolean get() = rule.pattern.isNotBlank()
}

/** The rules-file dialog: what importing the picked file would add and change, or why it can't be read. */
data class RuleImportPreview(
    val fileName: String,
    val error: RuleFileError? = null,
    val toAdd: Int = 0,
    val toUpdate: Int = 0,
    val unchanged: Int = 0,
    /** Entries without a text or category, or with an unknown match. */
    val invalid: Int = 0,
    /** Categories (or "Category › Subcategory") the household doesn't have; their rules are left out. */
    val unknownCategories: List<String> = emptyList(),
    /** Past transactions the file's rules would file somewhere else. */
    val pastToRefile: Int = 0,
) {
    val canImport: Boolean get() = error == null && toAdd + toUpdate + pastToRefile > 0
}

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
    val categories: List<Category> = emptyList(),
    val ruleEditor: RuleEditor? = null,
    val ruleImport: RuleImportPreview? = null,
)

class ManageViewModel(
    private val observeDatabase: ObserveDatabaseUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val setAskAccount: SetAskAccountOnImportUseCase,
    private val saveAlertSettings: SaveAlertSettingsUseCase,
    private val saveAssumptions: SaveAssumptionsUseCase,
    private val deleteRule: DeleteMerchantRuleUseCase,
    private val saveRule: SaveMerchantRuleUseCase,
    private val importRules: ImportMerchantRulesUseCase,
    private val addMember: AddMemberUseCase,
) : ViewModel() {
    private val editor = MutableStateFlow<AccountEditor?>(null)
    private val editedRule = MutableStateFlow<MerchantRule?>(null)
    private val ruleFile = MutableStateFlow<Pair<String, RuleFileContent>?>(null)

    val state: StateFlow<ManageUiState> = combine(observeDatabase(), editor, editedRule, ruleFile) { db, editor, rule, file ->
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
            categories = categories,
            ruleEditor = rule?.let {
                val matching = if (it.pattern.isBlank()) emptyList() else database.transactionsMatching(it)
                RuleEditor(
                    rule = it,
                    pastMatches = matching.size,
                    pastToRefile = matching.count { tx -> tx.categoryId != it.categoryId || tx.subcategoryId != it.subcategoryId },
                )
            },
            ruleImport = file?.let { (name, content) ->
                if (content.error != null) return@let RuleImportPreview(name, content.error)
                val plan = RuleFile.plan(content.entries, database.categories, database.merchantRules)
                RuleImportPreview(
                    fileName = name,
                    toAdd = plan.toAdd.size,
                    toUpdate = plan.toUpdate.size,
                    unchanged = plan.unchanged,
                    invalid = content.invalid,
                    unknownCategories = plan.unknownCategories,
                    pastToRefile = database.refiledBy(plan.rules).size,
                )
            },
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
        if (editedRule.value?.id == ruleId) editedRule.value = null
        viewModelScope.launch { deleteRule(ruleId) }
    }

    fun onEditRule(ruleId: String) {
        val database = (observeDatabase().value as? DatabaseState.Ready)?.database ?: return
        editedRule.value = database.merchantRules.firstOrNull { it.id == ruleId }
    }

    fun onRuleChange(transform: (MerchantRule) -> MerchantRule) {
        editedRule.update { it?.let(transform) }
    }

    /** Picking another category drops a subcategory that belonged to the old one. */
    fun onRuleCategory(categoryId: String) = onRuleChange { rule ->
        val keepsSub =
            state.value.categories.firstOrNull { it.id == categoryId }?.subcategories?.any { it.id == rule.subcategoryId } == true
        rule.copy(categoryId = categoryId, subcategoryId = rule.subcategoryId.takeIf { keepsSub })
    }

    fun onRuleMatch(match: RuleMatch) = onRuleChange { it.copy(match = match) }

    fun onDismissRule() {
        editedRule.value = null
    }

    /** Saves the edited rule; [applyToPast] also re-files every earlier transaction it matches. */
    fun onSaveRule(applyToPast: Boolean) {
        val rule = editedRule.value ?: return
        if (rule.pattern.isBlank()) return
        editedRule.value = null
        viewModelScope.launch { saveRule(rule, applyToPast) }
    }

    /** Reads a picked merchant rules file and shows what importing it would do. */
    fun onRuleFilePicked(files: List<PickedFile>) {
        val file = files.firstOrNull() ?: return
        ruleFile.value = file.name to RuleFile.read(file.bytes.decodeToString())
    }

    fun onRuleFilePickFailed() {
        ruleFile.value = "" to RuleFileContent(error = RuleFileError.NOT_JSON)
    }

    fun onDismissRuleImport() {
        ruleFile.value = null
    }

    /** Imports the picked rules file; [applyToPast] also re-files every earlier transaction its rules match. */
    fun onImportRules(applyToPast: Boolean) {
        val content = ruleFile.value?.second?.takeIf { it.error == null } ?: return
        ruleFile.value = null
        viewModelScope.launch { importRules(content.entries, applyToPast) }
    }
}
