package com.neteinstein.networth.feature.settings.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.ScreenHeader
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.components.SectionHeader
import com.neteinstein.networth.core.designsystem.ui.components.SyncIndicator
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.repository.DatabaseFile
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.domain.usecase.ShareDatabaseUseCase
import com.neteinstein.networth.feature.settings.generated.resources.Res
import com.neteinstein.networth.feature.settings.generated.resources.settings_account
import com.neteinstein.networth.feature.settings.generated.resources.settings_add_adult
import com.neteinstein.networth.feature.settings.generated.resources.settings_add_child
import com.neteinstein.networth.feature.settings.generated.resources.settings_adults
import com.neteinstein.networth.feature.settings.generated.resources.settings_appearance
import com.neteinstein.networth.feature.settings.generated.resources.settings_back
import com.neteinstein.networth.feature.settings.generated.resources.settings_cancel
import com.neteinstein.networth.feature.settings.generated.resources.settings_children
import com.neteinstein.networth.feature.settings.generated.resources.settings_database
import com.neteinstein.networth.feature.settings.generated.resources.settings_database_file
import com.neteinstein.networth.feature.settings.generated.resources.settings_dynamic_color
import com.neteinstein.networth.feature.settings.generated.resources.settings_dynamic_color_body
import com.neteinstein.networth.feature.settings.generated.resources.settings_household
import com.neteinstein.networth.feature.settings.generated.resources.settings_household_name
import com.neteinstein.networth.feature.settings.generated.resources.settings_member_name
import com.neteinstein.networth.feature.settings.generated.resources.settings_save
import com.neteinstein.networth.feature.settings.generated.resources.settings_share
import com.neteinstein.networth.feature.settings.generated.resources.settings_share_body
import com.neteinstein.networth.feature.settings.generated.resources.settings_share_email
import com.neteinstein.networth.feature.settings.generated.resources.settings_share_failed
import com.neteinstein.networth.feature.settings.generated.resources.settings_share_title
import com.neteinstein.networth.feature.settings.generated.resources.settings_shared
import com.neteinstein.networth.feature.settings.generated.resources.settings_sign_out
import com.neteinstein.networth.feature.settings.generated.resources.settings_switch_database
import com.neteinstein.networth.feature.settings.generated.resources.settings_switch_database_body
import com.neteinstein.networth.feature.settings.generated.resources.settings_sync_now
import com.neteinstein.networth.feature.settings.generated.resources.settings_theme
import com.neteinstein.networth.feature.settings.generated.resources.settings_theme_dark
import com.neteinstein.networth.feature.settings.generated.resources.settings_theme_light
import com.neteinstein.networth.feature.settings.generated.resources.settings_theme_system
import com.neteinstein.networth.feature.settings.generated.resources.settings_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        actions = SettingsActions(
            onBack = onBack,
            onThemeModeSelected = viewModel::onThemeModeSelected,
            onDynamicColorToggled = viewModel::onDynamicColorToggled,
            onShare = viewModel::onShare,
            onShareResultShown = viewModel::onShareResultShown,
            onSyncNow = viewModel::onSyncNow,
            onSwitchDatabase = viewModel::onSwitchDatabase,
            onRenameHousehold = viewModel::onRenameHousehold,
            onSaveMember = viewModel::onSaveMember,
            onSignOut = viewModel::onSignOut,
        ),
    )
}

data class SettingsActions(
    val onBack: () -> Unit = {},
    val onThemeModeSelected: (ThemeMode) -> Unit = {},
    val onDynamicColorToggled: (Boolean) -> Unit = {},
    val onShare: (String) -> Unit = {},
    val onShareResultShown: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onSwitchDatabase: () -> Unit = {},
    val onRenameHousehold: (String) -> Unit = {},
    val onSaveMember: (String?, String, MemberRole) -> Unit = { _, _, _ -> },
    val onSignOut: () -> Unit = {},
)

/** A member being renamed (id set) or added (id null). */
private data class MemberEdit(
    val id: String?,
    val name: String,
    val role: MemberRole,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    var memberEdit by remember { mutableStateOf<MemberEdit?>(null) }
    LaunchedEffect(state.shareResult) {
        val result = state.shareResult ?: return@LaunchedEffect
        snackbar.showSnackbar(getString(if (result == ShareResult.SHARED) Res.string.settings_shared else Res.string.settings_share_failed))
        actions.onShareResultShown()
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ScreenHeader(
                stringResource(Res.string.settings_title),
                stringResource(Res.string.settings_back),
                actions.onBack,
                Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 16.dp, bottom = 8.dp),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                if (state.file != null) {
                    DatabaseSection(state.file, state.sync, state.sharing, actions)
                    HouseholdSection(state, actions.onRenameHousehold) { memberEdit = it }
                }
                SectionHeader(stringResource(Res.string.settings_appearance))
                Text(
                    stringResource(Res.string.settings_theme),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.appearance.themeMode == mode,
                            onClick = { actions.onThemeModeSelected(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) { Text(stringResource(mode.label)) }
                    }
                }
                AnimatedVisibility(visible = state.dynamicColorAvailable) {
                    ListItem(
                        modifier = Modifier.padding(top = 16.dp),
                        headlineContent = { Text(stringResource(Res.string.settings_dynamic_color)) },
                        supportingContent = { Text(stringResource(Res.string.settings_dynamic_color_body)) },
                        trailingContent = {
                            Switch(checked = state.appearance.useDynamicColor, onCheckedChange = actions.onDynamicColorToggled)
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    )
                }
                if (state.email != null) {
                    SectionHeader(stringResource(Res.string.settings_account))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.email,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        OutlinedButton(onClick = actions.onSignOut) { Text(stringResource(Res.string.settings_sign_out)) }
                    }
                }
            }
        }
    }
    memberEdit?.let { edit ->
        MemberDialog(edit, onSave = { name ->
            memberEdit = null
            actions.onSaveMember(edit.id, name, edit.role)
        }, onDismiss = { memberEdit = null })
    }
}

@Composable
private fun DatabaseSection(file: DatabaseFile, sync: SyncStatus?, sharing: Boolean, actions: SettingsActions) {
    SectionHeader(stringResource(Res.string.settings_database))
    SectionCard(spacing = 10) {
        Text(
            stringResource(Res.string.settings_database_file),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(file.name, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (sync != null) SyncIndicator(sync, actions.onSyncNow, Modifier.weight(1f))
            TextButton(onClick = actions.onSyncNow) { Text(stringResource(Res.string.settings_sync_now)) }
        }
        Text(stringResource(Res.string.settings_share_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(Res.string.settings_share_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        var email by rememberSaveable { mutableStateOf("") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(Res.string.settings_share_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { actions.onShare(email) }, enabled = !sharing && ShareDatabaseUseCase.isEmail(email)) {
                Text(stringResource(Res.string.settings_share))
            }
        }
        Column(
            Modifier.fillMaxWidth().heightIn(
                min = 48.dp,
            ).clickable(role = Role.Button, onClick = actions.onSwitchDatabase).padding(vertical = 8.dp),
        ) {
            Text(
                stringResource(Res.string.settings_switch_database),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.settings_switch_database_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HouseholdSection(state: SettingsUiState, onRename: (String) -> Unit, onEditMember: (MemberEdit) -> Unit) {
    SectionHeader(stringResource(Res.string.settings_household))
    SectionCard(spacing = 8) {
        // Keyed on the file, not the name: saving trims, and re-keying on every save would eat typed spaces.
        var name by remember(state.file?.id) { mutableStateOf(state.householdName) }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onRename(it)
            },
            label = { Text(stringResource(Res.string.settings_household_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        MemberGroup(
            stringResource(Res.string.settings_adults),
            state.members.filter {
                it.role == MemberRole.ADULT
            },
            stringResource(Res.string.settings_add_adult),
            MemberRole.ADULT,
            onEditMember,
        )
        MemberGroup(
            stringResource(Res.string.settings_children),
            state.members.filter {
                it.role == MemberRole.CHILD
            },
            stringResource(Res.string.settings_add_child),
            MemberRole.CHILD,
            onEditMember,
        )
    }
}

@Composable
private fun MemberGroup(title: String, members: List<Member>, addLabel: String, role: MemberRole, onEdit: (MemberEdit) -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    members.forEach { member ->
        Text(
            member.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button) {
                onEdit(MemberEdit(member.id, member.name, member.role))
            }.padding(vertical = 10.dp),
        )
    }
    TextButton(onClick = { onEdit(MemberEdit(null, "", role)) }) { Text(addLabel) }
}

@Composable
private fun MemberDialog(edit: MemberEdit, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(edit.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_member_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(stringResource(Res.string.settings_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.settings_cancel)) } },
    )
}

private val ThemeMode.label: StringResource
    get() = when (this) {
        ThemeMode.SYSTEM -> Res.string.settings_theme_system
        ThemeMode.LIGHT -> Res.string.settings_theme_light
        ThemeMode.DARK -> Res.string.settings_theme_dark
    }

@Preview
@Composable
private fun SettingsContentPreview() {
    NetWorthTheme {
        SettingsContent(
            state = SettingsUiState(
                appearance = AppearanceSettings(ThemeMode.DARK, useDynamicColor = true),
                dynamicColorAvailable = true,
                email = "pedro@example.com",
                file = DatabaseFile("f", "Net Worth – The Vicentes.networth.json"),
                sync = SyncStatus.SYNCED,
                householdName = "The Vicentes",
                members = listOf(Member("p", "Pedro", MemberRole.ADULT), Member("i", "Inês", MemberRole.CHILD)),
            ),
            actions = SettingsActions(),
        )
    }
}
