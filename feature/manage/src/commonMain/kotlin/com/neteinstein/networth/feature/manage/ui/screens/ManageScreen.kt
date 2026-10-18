package com.neteinstein.networth.feature.manage.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.CardTitle
import com.neteinstein.networth.core.designsystem.ui.components.ColorKey
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.components.ScreenHeader
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.designsystem.ui.theme.categoryColor
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.Assumptions
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.feature.manage.generated.resources.Res
import com.neteinstein.networth.feature.manage.generated.resources.manage_accounts
import com.neteinstein.networth.feature.manage.generated.resources.manage_add
import com.neteinstein.networth.feature.manage.generated.resources.manage_alert_rules
import com.neteinstein.networth.feature.manage.generated.resources.manage_alert_rules_subscriptions
import com.neteinstein.networth.feature.manage.generated.resources.manage_alert_rules_summary
import com.neteinstein.networth.feature.manage.generated.resources.manage_ask_account
import com.neteinstein.networth.feature.manage.generated.resources.manage_ask_account_body
import com.neteinstein.networth.feature.manage.generated.resources.manage_assumptions
import com.neteinstein.networth.feature.manage.generated.resources.manage_assumptions_summary
import com.neteinstein.networth.feature.manage.generated.resources.manage_back
import com.neteinstein.networth.feature.manage.generated.resources.manage_categories
import com.neteinstein.networth.feature.manage.generated.resources.manage_categories_summary
import com.neteinstein.networth.feature.manage.generated.resources.manage_delete_rule
import com.neteinstein.networth.feature.manage.generated.resources.manage_no_accounts
import com.neteinstein.networth.feature.manage.generated.resources.manage_owner_both_adults
import com.neteinstein.networth.feature.manage.generated.resources.manage_owner_child
import com.neteinstein.networth.feature.manage.generated.resources.manage_rules
import com.neteinstein.networth.feature.manage.generated.resources.manage_rules_empty
import com.neteinstein.networth.feature.manage.generated.resources.manage_rules_learned
import com.neteinstein.networth.feature.manage.generated.resources.manage_rules_show_all
import com.neteinstein.networth.feature.manage.generated.resources.manage_rules_show_less
import com.neteinstein.networth.feature.manage.generated.resources.manage_saved_filters
import com.neteinstein.networth.feature.manage.generated.resources.manage_settings
import com.neteinstein.networth.feature.manage.generated.resources.manage_title
import com.neteinstein.networth.feature.manage.ui.components.AccountEditorDialog
import com.neteinstein.networth.feature.manage.ui.components.AlertRulesDialog
import com.neteinstein.networth.feature.manage.ui.components.AssumptionsDialog
import com.neteinstein.networth.feature.manage.ui.components.label
import com.neteinstein.networth.feature.manage.ui.components.percent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ManageScreen(
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    /** Opens straight into the assumptions editor (from Future's "Change assumptions"). */
    showAssumptions: Boolean = false,
    viewModel: ManageViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ManageContent(
        state = state,
        showAssumptionsAtStart = showAssumptions,
        actions = ManageActions(
            onBack = onBack,
            onOpenCategories = onOpenCategories,
            onOpenSettings = onOpenSettings,
            onAddAccount = viewModel::onAddAccount,
            onEditAccount = viewModel::onEditAccount,
            onEditorChange = viewModel::onEditorChange,
            onAddPerson = viewModel::onAddPerson,
            onDismissEditor = viewModel::onDismissEditor,
            onSaveAccount = viewModel::onSaveAccount,
            onDeleteAccount = viewModel::onDeleteAccount,
            onAskAccountChange = viewModel::onAskAccountChange,
            onSaveAlerts = viewModel::onSaveAlerts,
            onSaveAssumptions = viewModel::onSaveAssumptions,
            onDeleteRule = viewModel::onDeleteRule,
        ),
    )
}

data class ManageActions(
    val onBack: () -> Unit = {},
    val onOpenCategories: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onAddAccount: () -> Unit = {},
    val onEditAccount: (String) -> Unit = {},
    val onEditorChange: ((AccountEditor) -> AccountEditor) -> Unit = {},
    val onAddPerson: (String, MemberRole) -> Unit = { _, _ -> },
    val onDismissEditor: () -> Unit = {},
    val onSaveAccount: () -> Unit = {},
    val onDeleteAccount: () -> Unit = {},
    val onAskAccountChange: (Boolean) -> Unit = {},
    val onSaveAlerts: (Int, Long, Boolean) -> Unit = { _, _, _ -> },
    val onSaveAssumptions: (Assumptions) -> Unit = {},
    val onDeleteRule: (String) -> Unit = {},
)

@Composable
fun ManageContent(
    state: ManageUiState,
    actions: ManageActions,
    modifier: Modifier = Modifier,
    showAssumptionsAtStart: Boolean = false,
) {
    var showAlertRules by rememberSaveable { mutableStateOf(false) }
    var showAssumptions by rememberSaveable { mutableStateOf(showAssumptionsAtStart) }
    val currency = CurrencyCode.of(state.currencyCode)
    Scaffold(modifier = modifier) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom =
                        padding.calculateBottomPadding() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    ScreenHeader(stringResource(Res.string.manage_title), stringResource(Res.string.manage_back), actions.onBack) {
                        OutlinedIconButton(onClick = actions.onOpenSettings, modifier = Modifier.size(44.dp)) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(Res.string.manage_settings),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                val card = Modifier.padding(horizontal = 20.dp)
                item { AccountsCard(state, currency, actions, card) }
                item { CategoriesLink(state, actions.onOpenCategories, card) }
                item { RulesCard(state, actions.onDeleteRule, card) }
                if (state.savedFilters.isNotEmpty()) {
                    item {
                        SectionCard(card, spacing = 6) {
                            CardTitle(stringResource(Res.string.manage_saved_filters))
                            state.savedFilters.forEach {
                                Text(it.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.heightIn(min = 40.dp))
                            }
                        }
                    }
                }
                item {
                    val summary =
                        stringResource(
                            Res.string.manage_alert_rules_summary,
                            state.alerts.spikeThresholdPct,
                            MoneyFormatter.whole(state.alerts.largePaymentMinor, currency),
                        )
                    LinkCard(
                        title = stringResource(Res.string.manage_alert_rules),
                        subtitle = if (state.alerts.newSubscriptions) {
                            stringResource(
                                Res.string.manage_alert_rules_subscriptions,
                                summary,
                            )
                        } else {
                            summary
                        },
                        onClick = { showAlertRules = true },
                        modifier = card,
                    )
                }
                item {
                    val a = state.assumptions
                    LinkCard(
                        title = stringResource(Res.string.manage_assumptions),
                        subtitle = stringResource(
                            Res.string.manage_assumptions_summary,
                            percent(a.returnRatePct),
                            percent(a.incomeGrowthPct),
                            percent(a.priceRisePct),
                            a.emergencyFundMonths,
                        ),
                        onClick = { showAssumptions = true },
                        modifier = card,
                    )
                }
            }
        }
    }
    state.editor?.let { editor ->
        AccountEditorDialog(
            editor,
            state.members,
            actions.onEditorChange,
            actions.onAddPerson,
            actions.onSaveAccount,
            actions.onDeleteAccount,
            actions.onDismissEditor,
        )
    }
    if (showAlertRules) {
        AlertRulesDialog(
            alerts = state.alerts,
            currency = currency,
            onSave = { threshold, large, subscriptions ->
                showAlertRules = false
                actions.onSaveAlerts(threshold, large, subscriptions)
            },
            onDismiss = { showAlertRules = false },
        )
    }
    if (showAssumptions && !state.loading) {
        AssumptionsDialog(
            assumptions = state.assumptions,
            onSave = {
                showAssumptions = false
                actions.onSaveAssumptions(it)
            },
            onDismiss = { showAssumptions = false },
        )
    }
}

@Composable
private fun AccountsCard(state: ManageUiState, currency: CurrencyCode, actions: ManageActions, modifier: Modifier) {
    SectionCard(modifier, spacing = 4) {
        CardTitle(
            stringResource(Res.string.manage_accounts),
            action = stringResource(Res.string.manage_add),
            onAction = actions.onAddAccount,
        )
        if (state.accounts.isEmpty()) {
            Text(
                stringResource(Res.string.manage_no_accounts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.accounts.forEach { managed ->
            AccountRow(managed, state.members, currency) { actions.onEditAccount(managed.account.id) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.manage_ask_account), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(Res.string.manage_ask_account_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = state.askAccountOnImport, onCheckedChange = actions.onAskAccountChange)
        }
    }
}

@Composable
private fun AccountRow(managed: ManagedAccount, members: List<Member>, currency: CurrencyCode, onClick: () -> Unit) {
    val finance = NetWorthTheme.financeColors
    val account = managed.account
    val (avatar, onAvatar) = when {
        account.type.isLiability -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
        managed.isChild -> finance.warningContainer to finance.onWarningContainer
        account.type == AccountType.CURRENT -> finance.heroContainer to finance.onHeroContainer
        else -> finance.goodContainer to finance.onGoodContainer
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(avatar, CircleShape), contentAlignment = Alignment.Center) {
            Text(
                account.name.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = onAvatar,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(account.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                accountKind(account, managed.isChild, members),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val balance = if (account.type.isLiability) -managed.balanceMinor else managed.balanceMinor
        Text(MoneyFormatter.whole(balance, currency), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun accountKind(account: Account, isChild: Boolean, members: List<Member>): String {
    val owners = account.ownerIds.mapNotNull { id -> members.firstOrNull { it.id == id } }
    val adults = members.filter { it.role == MemberRole.ADULT }
    val who = when {
        isChild -> stringResource(Res.string.manage_owner_child)
        adults.size > 1 &&
            owners.size == adults.size &&
            owners.all {
                it.role == MemberRole.ADULT
            } -> stringResource(Res.string.manage_owner_both_adults)
        else -> owners.joinToString(", ") { it.name }
    }
    // An account named after its owner ("Pedro") doesn't repeat the name.
    val parts = listOfNotNull(stringResource(account.type.label), who.takeIf { it.isNotEmpty() && it != account.name }, account.bank)
    return parts.joinToString(" · ")
}

@Composable
private fun CategoriesLink(state: ManageUiState, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp)) {
                state.categoryColors.forEachIndexed { i, hex ->
                    ColorKey(categoryColor(hex), Modifier.offset(x = (i % 2 * 20).dp, y = (i / 2 * 20).dp), size = 18.dp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.manage_categories), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(Res.string.manage_categories_summary, state.categoryCount, state.subcategoryCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(NetWorthIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RulesCard(state: ManageUiState, onDelete: (String) -> Unit, modifier: Modifier) {
    var showAll by remember { mutableStateOf(false) }
    SectionCard(modifier, spacing = 4) {
        CardTitle(stringResource(Res.string.manage_rules), trailing = stringResource(Res.string.manage_rules_learned, state.rules.size))
        if (state.rules.isEmpty()) {
            Text(
                stringResource(Res.string.manage_rules_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        (if (showAll) state.rules else state.rules.take(3)).forEach { rule ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    rule.pattern,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = NetWorthTheme.monoFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ColorKey(categoryColor(rule.colorHex), size = 8.dp)
                Text(rule.categoryName, style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = { onDelete(rule.id) }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.manage_delete_rule, rule.pattern),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (state.rules.size > 3) {
            TextButton(onClick = { showAll = !showAll }) {
                Text(
                    if (showAll) {
                        stringResource(
                            Res.string.manage_rules_show_less,
                        )
                    } else {
                        stringResource(Res.string.manage_rules_show_all, state.rules.size)
                    },
                )
            }
        }
    }
}

@Composable
private fun LinkCard(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(NetWorthIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview
@Composable
private fun ManagePreview() {
    val pedro = Member("p", "Pedro", MemberRole.ADULT)
    val maria = Member("m", "Maria", MemberRole.ADULT)
    val ines = Member("i", "Inês", MemberRole.CHILD)
    NetWorthTheme {
        ManageContent(
            ManageUiState(
                loading = false,
                accounts = listOf(
                    ManagedAccount(Account("a", "Pedro", AccountType.CURRENT, listOf("p"), bank = "novobanco"), 2_140_00, false, true),
                    ManagedAccount(Account("j", "Joint savings", AccountType.SAVINGS, listOf("p", "m")), 12_600_00, false, false),
                    ManagedAccount(Account("i", "Inês", AccountType.SAVINGS, listOf("i")), 2_300_00, true, false),
                    ManagedAccount(Account("h", "Mortgage", AccountType.MORTGAGE, listOf("p", "m")), 147_268_16, false, false),
                ),
                members = listOf(pedro, maria, ines),
                categoryCount = 10,
                subcategoryCount = 39,
                categoryColors = listOf("#141B2D", "#2D55D6", "#D9652B", "#7D9BF2"),
                rules = listOf(
                    RuleLine("r", "CONTINENTE*", "Groceries", "#2D55D6"),
                    RuleLine("s", "DECATHLON*", "Kids’ activities", "#F0A964"),
                ),
            ),
            ManageActions(),
        )
    }
}
