package com.neteinstein.networth.feature.onboarding.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.OverlineLabel
import com.neteinstein.networth.core.designsystem.ui.components.ScreenHeader
import com.neteinstein.networth.core.designsystem.ui.components.SegmentedControl
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.repository.DatabaseFileName
import com.neteinstein.networth.core.domain.repository.DatabaseFolder
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.feature.onboarding.domain.model.HouseholdForm
import com.neteinstein.networth.feature.onboarding.generated.resources.Res
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_add_adult
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_add_child
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_adult_name
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_adults
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_app_name
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_back
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_child_name
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_children
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_choose_body
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_choose_folder
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_choose_title
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_create
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_create_accounts
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_create_accounts_body
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_currency
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_error_drive_api_disabled
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_error_drive_full
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_error_network
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_error_newer_schema
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_error_no_drive_permission
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_error_not_a_database
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_error_not_accessible
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_error_not_configured
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_error_sign_in
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_file
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_file_auto
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_file_custom
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_file_name
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_file_question
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_file_saved_as
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_folder
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_folder_my_drive
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_form_title
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_household_hint
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_household_name
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_joint_savings
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_open_file
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_open_file_body
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_privacy
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_remove
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_sign_in
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_signed_in_as
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_start_fresh
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_start_fresh_body
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_tagline
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_use_another_account
import com.neteinstein.networth.feature.onboarding.generated.resources.onboarding_use_my_drive
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val jointSavings = stringResource(Res.string.onboarding_joint_savings)
    OnboardingContent(
        state = state,
        actions = OnboardingActions(
            onSignIn = viewModel::onSignIn,
            onStartFresh = viewModel::onStartFresh,
            onOpenExisting = viewModel::onOpenExisting,
            onBack = viewModel::onBackToChoices,
            onSignOut = viewModel::onSignOut,
            onCreate = { viewModel.onCreate(jointSavings) },
            onErrorShown = viewModel::onErrorShown,
            onHouseholdNameChange = viewModel::onHouseholdNameChange,
            onAdultChange = viewModel::onAdultChange,
            onAddAdult = viewModel::onAddAdult,
            onRemoveAdult = viewModel::onRemoveAdult,
            onChildChange = viewModel::onChildChange,
            onAddChild = viewModel::onAddChild,
            onRemoveChild = viewModel::onRemoveChild,
            onCurrencySelected = viewModel::onCurrencySelected,
            onCreateAccountsChange = viewModel::onCreateAccountsChange,
            onChooseFileChange = viewModel::onChooseFileChange,
            onFileNameChange = viewModel::onFileNameChange,
            onPickFolder = viewModel::onPickFolder,
            onUseMyDrive = viewModel::onUseMyDrive,
        ),
    )
}

/** Every callback the onboarding screens raise, grouped so previews and tests can pass no-ops. */
data class OnboardingActions(
    val onSignIn: () -> Unit = {},
    val onStartFresh: () -> Unit = {},
    val onOpenExisting: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onCreate: () -> Unit = {},
    val onErrorShown: () -> Unit = {},
    val onHouseholdNameChange: (String) -> Unit = {},
    val onAdultChange: (Int, String) -> Unit = { _, _ -> },
    val onAddAdult: () -> Unit = {},
    val onRemoveAdult: (Int) -> Unit = {},
    val onChildChange: (Int, String) -> Unit = { _, _ -> },
    val onAddChild: () -> Unit = {},
    val onRemoveChild: (Int) -> Unit = {},
    val onCurrencySelected: (String) -> Unit = {},
    val onCreateAccountsChange: (Boolean) -> Unit = {},
    val onChooseFileChange: (Boolean) -> Unit = {},
    val onFileNameChange: (String) -> Unit = {},
    val onPickFolder: () -> Unit = {},
    val onUseMyDrive: () -> Unit = {},
)

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        snackbar.showSnackbar(getString(error.message))
        actions.onErrorShown()
    }
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            ) { step ->
                when (step) {
                    OnboardingStep.SIGN_IN -> SignInStep(state.busy, actions.onSignIn)
                    OnboardingStep.CHOOSE -> ChooseStep(state.user, state.busy, actions)
                    OnboardingStep.START_FRESH -> StartFreshStep(state.form, state.busy, actions)
                }
            }
        }
    }
}

private val OnboardingError.message: StringResource
    get() = when (this) {
        OnboardingError.NOT_CONFIGURED -> Res.string.onboarding_error_not_configured
        OnboardingError.SIGN_IN_FAILED -> Res.string.onboarding_error_sign_in
        OnboardingError.NOT_A_DATABASE -> Res.string.onboarding_error_not_a_database
        OnboardingError.NEWER_SCHEMA -> Res.string.onboarding_error_newer_schema
        OnboardingError.NOT_ACCESSIBLE -> Res.string.onboarding_error_not_accessible
        OnboardingError.NO_DRIVE_PERMISSION -> Res.string.onboarding_error_no_drive_permission
        OnboardingError.DRIVE_API_DISABLED -> Res.string.onboarding_error_drive_api_disabled
        OnboardingError.DRIVE_FULL -> Res.string.onboarding_error_drive_full
        OnboardingError.NETWORK -> Res.string.onboarding_error_network
    }

@Composable
private fun SignInStep(busy: Boolean, onSignIn: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.heightIn(min = 72.dp))
        Text(
            stringResource(Res.string.onboarding_app_name),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(Res.string.onboarding_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.heightIn(min = 24.dp))
        BusyButton(stringResource(Res.string.onboarding_sign_in), busy, onSignIn)
        Text(
            stringResource(Res.string.onboarding_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChooseStep(user: UserProfile?, busy: Boolean, actions: OnboardingActions) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            stringResource(Res.string.onboarding_choose_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(Res.string.onboarding_choose_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChoiceCard(
            stringResource(Res.string.onboarding_start_fresh),
            stringResource(Res.string.onboarding_start_fresh_body),
            !busy,
            actions.onStartFresh,
        )
        ChoiceCard(
            stringResource(Res.string.onboarding_open_file),
            stringResource(Res.string.onboarding_open_file_body),
            !busy,
            actions.onOpenExisting,
        )
        if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        if (user != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.onboarding_signed_in_as, user.email),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.onSignOut, enabled = !busy) { Text(stringResource(Res.string.onboarding_use_another_account)) }
            }
        }
    }
}

@Composable
private fun ChoiceCard(title: String, body: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StartFreshStep(form: HouseholdForm, busy: Boolean, actions: OnboardingActions) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(
            stringResource(Res.string.onboarding_form_title),
            stringResource(Res.string.onboarding_back),
            actions.onBack,
            horizontalPadding = 0.dp,
        )
        OutlinedTextField(
            value = form.householdName,
            onValueChange = actions.onHouseholdNameChange,
            label = { Text(stringResource(Res.string.onboarding_household_name)) },
            placeholder = { Text(stringResource(Res.string.onboarding_household_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        PeopleSection(
            title = stringResource(Res.string.onboarding_adults),
            names = form.adults,
            fieldLabel = Res.string.onboarding_adult_name,
            addLabel = stringResource(Res.string.onboarding_add_adult),
            canRemove = form.adults.size > 1,
            onChange = actions.onAdultChange,
            onAdd = actions.onAddAdult,
            onRemove = actions.onRemoveAdult,
        )
        PeopleSection(
            title = stringResource(Res.string.onboarding_children),
            names = form.children,
            fieldLabel = Res.string.onboarding_child_name,
            addLabel = stringResource(Res.string.onboarding_add_child),
            canRemove = true,
            onChange = actions.onChildChange,
            onAdd = actions.onAddChild,
            onRemove = actions.onRemoveChild,
        )
        OverlineLabel(stringResource(Res.string.onboarding_currency))
        SegmentedControl(
            options = CurrencyCode.KNOWN.map { it.code },
            selected = form.currencyCode,
            label = { it },
            onSelect = actions.onCurrencySelected,
        )
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(checked = form.createAccounts, onCheckedChange = actions.onCreateAccountsChange)
            Column(Modifier.padding(top = 12.dp)) {
                Text(stringResource(Res.string.onboarding_create_accounts), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(Res.string.onboarding_create_accounts_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FileSection(form, busy, actions)
        BusyButton(stringResource(Res.string.onboarding_create), busy, actions.onCreate, enabled = form.canSubmit)
    }
}

/** Asks whether the user wants to pick the new file's name and folder, or leave both to the app. */
@Composable
private fun FileSection(form: HouseholdForm, busy: Boolean, actions: OnboardingActions) {
    val myDrive = stringResource(Res.string.onboarding_folder_my_drive)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OverlineLabel(stringResource(Res.string.onboarding_file))
        Text(
            stringResource(Res.string.onboarding_file_question),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedControl(
            options = listOf(false, true),
            selected = form.chooseFile,
            label = { stringResource(if (it) Res.string.onboarding_file_custom else Res.string.onboarding_file_auto) },
            onSelect = actions.onChooseFileChange,
        )
        if (form.chooseFile) {
            OutlinedTextField(
                value = form.fileName,
                onValueChange = actions.onFileNameChange,
                label = { Text(stringResource(Res.string.onboarding_file_name)) },
                placeholder = { Text(DatabaseFileName.forHousehold(form.householdName)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.onboarding_folder),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(form.folder?.name ?: myDrive, style = MaterialTheme.typography.bodyMedium)
                }
                if (form.folder != null) {
                    IconButton(onClick = actions.onUseMyDrive, enabled = !busy) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.onboarding_use_my_drive))
                    }
                }
                TextButton(onClick = actions.onPickFolder, enabled = !busy) { Text(stringResource(Res.string.onboarding_choose_folder)) }
            }
        }
        Text(
            stringResource(
                Res.string.onboarding_file_saved_as,
                form.resolvedFileName,
                form.folder?.takeIf { form.chooseFile }?.name ?: myDrive,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PeopleSection(
    title: String,
    names: List<String>,
    fieldLabel: StringResource,
    addLabel: String,
    canRemove: Boolean,
    onChange: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OverlineLabel(title)
        names.forEachIndexed { index, name ->
            val label = stringResource(fieldLabel, index + 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { onChange(index, it) },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1f),
                )
                if (canRemove) {
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.onboarding_remove, name.ifBlank { label }))
                    }
                }
            }
        }
        TextButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(addLabel, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun BusyButton(label: String, busy: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled && !busy, shape = CircleShape, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(label)
    }
}

@Preview
@Composable
private fun SignInPreview() {
    NetWorthTheme { OnboardingContent(OnboardingUiState(), OnboardingActions()) }
}

@Preview
@Composable
private fun ChoosePreview() {
    NetWorthTheme {
        OnboardingContent(OnboardingUiState(OnboardingStep.CHOOSE, UserProfile("1", "pedro@example.com", "Pedro")), OnboardingActions())
    }
}

@Preview
@Composable
private fun StartFreshChooseFilePreview() {
    NetWorthTheme {
        OnboardingContent(
            OnboardingUiState(
                step = OnboardingStep.START_FRESH,
                form = HouseholdForm(
                    householdName = "The Vicentes",
                    adults = listOf("Pedro"),
                    chooseFile = true,
                    fileName = "Our money",
                    folder = DatabaseFolder("folder-1", "Finances"),
                ),
            ),
            OnboardingActions(),
        )
    }
}

@Preview
@Composable
private fun StartFreshPreview() {
    NetWorthTheme(darkTheme = true) {
        OnboardingContent(
            OnboardingUiState(
                step = OnboardingStep.START_FRESH,
                form = HouseholdForm("The Vicentes", listOf("Pedro", "Maria"), listOf("Inês", "")),
            ),
            OnboardingActions(),
        )
    }
}
