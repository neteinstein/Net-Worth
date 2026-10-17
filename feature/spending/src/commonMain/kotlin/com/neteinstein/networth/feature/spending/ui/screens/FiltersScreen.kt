package com.neteinstein.networth.feature.spending.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.ChoiceChip
import com.neteinstein.networth.core.designsystem.ui.components.ColorKey
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.components.OverlineLabel
import com.neteinstein.networth.core.designsystem.ui.components.SegmentedControl
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.designsystem.ui.theme.colorFromHex
import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.SavedFilter
import com.neteinstein.networth.core.domain.model.TransactionTypeFilter
import com.neteinstein.networth.feature.spending.generated.resources.Res
import com.neteinstein.networth.feature.spending.generated.resources.filters_accounts
import com.neteinstein.networth.feature.spending.generated.resources.filters_adults
import com.neteinstein.networth.feature.spending.generated.resources.filters_back
import com.neteinstein.networth.feature.spending.generated.resources.filters_both
import com.neteinstein.networth.feature.spending.generated.resources.filters_categories
import com.neteinstein.networth.feature.spending.generated.resources.filters_everyone
import com.neteinstein.networth.feature.spending.generated.resources.filters_expenses
import com.neteinstein.networth.feature.spending.generated.resources.filters_include_transfers
import com.neteinstein.networth.feature.spending.generated.resources.filters_income
import com.neteinstein.networth.feature.spending.generated.resources.filters_invalid_amount
import com.neteinstein.networth.feature.spending.generated.resources.filters_max
import com.neteinstein.networth.feature.spending.generated.resources.filters_min
import com.neteinstein.networth.feature.spending.generated.resources.filters_name
import com.neteinstein.networth.feature.spending.generated.resources.filters_reset
import com.neteinstein.networth.feature.spending.generated.resources.filters_save
import com.neteinstein.networth.feature.spending.generated.resources.filters_save_title
import com.neteinstein.networth.feature.spending.generated.resources.filters_saved
import com.neteinstein.networth.feature.spending.generated.resources.filters_show
import com.neteinstein.networth.feature.spending.generated.resources.filters_tagged_for
import com.neteinstein.networth.feature.spending.generated.resources.filters_text
import com.neteinstein.networth.feature.spending.generated.resources.filters_title
import com.neteinstein.networth.feature.spending.generated.resources.filters_type
import com.neteinstein.networth.feature.spending.generated.resources.spending_cancel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FiltersScreen(
    onBack: () -> Unit,
    onShowTransactions: () -> Unit,
    viewModel: FiltersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FiltersContent(
        state = state,
        actions =
            FiltersActions(
                onBack = onBack,
                onReset = viewModel::onReset,
                onAccount = viewModel::onAccountToggled,
                onCategory = viewModel::onCategoryToggled,
                onTagged = viewModel::onTaggedToggled,
                onType = viewModel::onTypeSelected,
                onTransfers = viewModel::onIncludeTransfers,
                onMin = viewModel::onMinChanged,
                onMax = viewModel::onMaxChanged,
                onText = viewModel::onTextChanged,
                onSave = viewModel::onSave,
                onApplySaved = viewModel::onApplySaved,
                onShow = onShowTransactions,
            ),
    )
}

data class FiltersActions(
    val onBack: () -> Unit = {},
    val onReset: () -> Unit = {},
    val onAccount: (String) -> Unit = {},
    val onCategory: (String) -> Unit = {},
    val onTagged: (TaggedFor) -> Unit = {},
    val onType: (TransactionTypeFilter) -> Unit = {},
    val onTransfers: (Boolean) -> Unit = {},
    val onMin: (String) -> Unit = {},
    val onMax: (String) -> Unit = {},
    val onText: (String) -> Unit = {},
    val onSave: (String) -> Unit = {},
    val onApplySaved: (SavedFilter) -> Unit = {},
    val onShow: () -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersContent(
    state: FiltersUiState,
    actions: FiltersActions,
    modifier: Modifier = Modifier,
) {
    var saving by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (state.ready) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = { saving = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(Res.string.filters_save))
                    }
                    Button(onClick = actions.onShow, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(pluralStringResource(Res.plurals.filters_show, state.matchCount, state.matchCount))
                    }
                }
            }
        },
    ) { padding ->
        if (!state.ready) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val filter = state.filter
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom =
                        padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                val section = Modifier.padding(horizontal = 20.dp)
                item {
                    Row(section.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedIconButton(onClick = actions.onBack, modifier = Modifier.size(44.dp)) {
                            Icon(
                                NetWorthIcons.ChevronLeft,
                                contentDescription = stringResource(Res.string.filters_back),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            stringResource(Res.string.filters_title),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f).padding(start = 8.dp).semantics { heading() },
                        )
                        TextButton(onClick = actions.onReset) { Text(stringResource(Res.string.filters_reset)) }
                    }
                }
                if (state.savedFilters.isNotEmpty()) {
                    item {
                        Group(stringResource(Res.string.filters_saved), section) {
                            state.savedFilters.forEach { saved ->
                                ChoiceChip(saved.name, selected = saved.filter == filter, onClick = { actions.onApplySaved(saved) })
                            }
                        }
                    }
                }
                item {
                    Group(stringResource(Res.string.filters_accounts), section) {
                        state.accounts.forEach { account ->
                            ChoiceChip(account.name, selected = state.isAccountSelected(account.id), onClick = {
                                actions.onAccount(account.id)
                            })
                        }
                    }
                }
                item {
                    Group(stringResource(Res.string.filters_categories), section) {
                        state.categories.forEach { category ->
                            ChoiceChip(
                                category.name,
                                selected = category.id in filter.categoryIds,
                                onClick = { actions.onCategory(category.id) },
                                leading = { ColorKey(colorFromHex(category.colorHex)) },
                            )
                        }
                    }
                }
                item {
                    Group(stringResource(Res.string.filters_tagged_for), section) {
                        ChoiceChip(stringResource(Res.string.filters_everyone), TaggedFor.Everyone in state.tagged, {
                            actions.onTagged(TaggedFor.Everyone)
                        })
                        ChoiceChip(stringResource(Res.string.filters_adults), TaggedFor.Adults in state.tagged, {
                            actions.onTagged(TaggedFor.Adults)
                        })
                        state.children.forEach { child ->
                            val choice = TaggedFor.Person(child.id)
                            ChoiceChip(child.name, choice in state.tagged, { actions.onTagged(choice) })
                        }
                    }
                }
                item {
                    Column(section, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OverlineLabel(stringResource(Res.string.filters_type))
                        SegmentedControl(
                            options = TransactionTypeFilter.entries,
                            selected = filter.type,
                            label = {
                                stringResource(
                                    when (it) {
                                        TransactionTypeFilter.EXPENSES -> Res.string.filters_expenses
                                        TransactionTypeFilter.INCOME -> Res.string.filters_income
                                        TransactionTypeFilter.BOTH -> Res.string.filters_both
                                    },
                                )
                            },
                            onSelect = actions.onType,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(Res.string.filters_include_transfers),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(checked = filter.includeTransfers, onCheckedChange = actions.onTransfers)
                        }
                    }
                }
                item {
                    Row(section, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AmountField(
                            stringResource(Res.string.filters_min),
                            state.minText,
                            state.minValid,
                            actions.onMin,
                            Modifier.weight(1f),
                        )
                        AmountField(
                            stringResource(Res.string.filters_max),
                            state.maxText,
                            state.maxValid,
                            actions.onMax,
                            Modifier.weight(1f),
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = filter.text.orEmpty(),
                        onValueChange = actions.onText,
                        label = { Text(stringResource(Res.string.filters_text)) },
                        singleLine = true,
                        modifier = section.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (saving) {
        SaveFilterDialog(
            onSave = { name ->
                saving = false
                actions.onSave(name)
            },
            onDismiss = { saving = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Group(
    title: String,
    modifier: Modifier,
    chips: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OverlineLabel(title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    valid: Boolean,
    onChange: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = !valid,
        supportingText = if (valid) null else ({ Text(stringResource(Res.string.filters_invalid_amount)) }),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun SaveFilterDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.filters_save_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.filters_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(stringResource(Res.string.filters_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.spending_cancel)) } },
    )
}

@Preview
@Composable
private fun FiltersPreview() {
    NetWorthTheme {
        FiltersContent(
            FiltersUiState(
                ready = true,
                accounts = listOf(Account("a1", "Pedro", AccountType.CURRENT), Account("j", "Joint savings", AccountType.SAVINGS)),
                categories = previewCategories(),
                children = previewMembers().drop(1),
                matchCount = 35,
            ),
            FiltersActions(),
        )
    }
}
