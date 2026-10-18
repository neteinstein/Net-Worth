package com.neteinstein.networth.feature.manage.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.ui.components.OverlineLabel
import com.neteinstein.networth.core.designsystem.ui.components.OwnerPicker
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.format.MoneyParser
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.AlertSettings
import com.neteinstein.networth.core.domain.model.Assumptions
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.feature.manage.generated.resources.Res
import com.neteinstein.networth.feature.manage.generated.resources.manage_account_balance
import com.neteinstein.networth.feature.manage.generated.resources.manage_account_balance_invalid
import com.neteinstein.networth.feature.manage.generated.resources.manage_account_bank
import com.neteinstein.networth.feature.manage.generated.resources.manage_account_delete_blocked
import com.neteinstein.networth.feature.manage.generated.resources.manage_account_iban
import com.neteinstein.networth.feature.manage.generated.resources.manage_account_name
import com.neteinstein.networth.feature.manage.generated.resources.manage_account_number
import com.neteinstein.networth.feature.manage.generated.resources.manage_account_owners
import com.neteinstein.networth.feature.manage.generated.resources.manage_account_type
import com.neteinstein.networth.feature.manage.generated.resources.manage_add_account
import com.neteinstein.networth.feature.manage.generated.resources.manage_alert_rules
import com.neteinstein.networth.feature.manage.generated.resources.manage_assumption_fund
import com.neteinstein.networth.feature.manage.generated.resources.manage_assumption_income
import com.neteinstein.networth.feature.manage.generated.resources.manage_assumption_prices
import com.neteinstein.networth.feature.manage.generated.resources.manage_assumption_return
import com.neteinstein.networth.feature.manage.generated.resources.manage_assumptions
import com.neteinstein.networth.feature.manage.generated.resources.manage_assumptions_body
import com.neteinstein.networth.feature.manage.generated.resources.manage_cancel
import com.neteinstein.networth.feature.manage.generated.resources.manage_delete
import com.neteinstein.networth.feature.manage.generated.resources.manage_edit_account
import com.neteinstein.networth.feature.manage.generated.resources.manage_large_payment
import com.neteinstein.networth.feature.manage.generated.resources.manage_new_subscriptions
import com.neteinstein.networth.feature.manage.generated.resources.manage_save
import com.neteinstein.networth.feature.manage.generated.resources.manage_spike_threshold
import com.neteinstein.networth.feature.manage.ui.screens.AccountEditor
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountEditorDialog(
    editor: AccountEditor,
    members: List<Member>,
    onChange: ((AccountEditor) -> AccountEditor) -> Unit,
    onAddPerson: (String, MemberRole) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editor.id == null) Res.string.manage_add_account else Res.string.manage_edit_account)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    label = { Text(stringResource(Res.string.manage_account_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                var typeMenu by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = it }) {
                    OutlinedTextField(
                        value = stringResource(editor.type.label),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.manage_account_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        AccountType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(stringResource(type.label)) },
                                onClick = {
                                    typeMenu = false
                                    onChange { it.copy(type = type) }
                                },
                            )
                        }
                    }
                }
                OverlineLabel(stringResource(Res.string.manage_account_owners))
                OwnerPicker(
                    members = members,
                    selectedIds = editor.ownerIds,
                    onToggle = { id -> onChange { it.copy(ownerIds = if (id in it.ownerIds) it.ownerIds - id else it.ownerIds + id) } },
                    onAddPerson = onAddPerson,
                )
                OutlinedTextField(
                    value = editor.bank,
                    onValueChange = { value -> onChange { it.copy(bank = value) } },
                    label = { Text(stringResource(Res.string.manage_account_bank)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.accountNumber,
                    onValueChange = { value -> onChange { it.copy(accountNumber = value) } },
                    label = { Text(stringResource(Res.string.manage_account_number)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.iban,
                    onValueChange = { value -> onChange { it.copy(iban = value) } },
                    label = { Text(stringResource(Res.string.manage_account_iban)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.balance,
                    onValueChange = { value -> onChange { it.copy(balance = value) } },
                    label = { Text(stringResource(Res.string.manage_account_balance)) },
                    isError = !editor.balanceIsValid,
                    supportingText = if (editor.balanceIsValid) {
                        null
                    } else {
                        (
                            {
                                Text(stringResource(Res.string.manage_account_balance_invalid))
                            }
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (editor.id != null && !editor.canDelete) {
                    Text(
                        stringResource(Res.string.manage_account_delete_blocked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = editor.canSave) { Text(stringResource(Res.string.manage_save)) } },
        dismissButton = {
            Row {
                if (editor.canDelete) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(Res.string.manage_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.manage_cancel)) }
            }
        },
    )
}

@Composable
internal fun AlertRulesDialog(
    alerts: AlertSettings,
    currency: CurrencyCode,
    onSave: (Int, Long, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var threshold by remember { mutableFloatStateOf(alerts.spikeThresholdPct.toFloat()) }
    var large by remember {
        mutableStateOf(MoneyFormatter.format(alerts.largePaymentMinor, currency, showFraction = false).removePrefix(currency.symbol))
    }
    var subscriptions by remember { mutableStateOf(alerts.newSubscriptions) }
    val largeMinor = MoneyParser.parseMinor(large)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.manage_alert_rules)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.manage_spike_threshold, threshold.roundToInt()), style = MaterialTheme.typography.bodyMedium)
                Slider(value = threshold, onValueChange = { threshold = it }, valueRange = 10f..100f, steps = 17)
                OutlinedTextField(
                    value = large,
                    onValueChange = { large = it },
                    label = { Text(stringResource(Res.string.manage_large_payment)) },
                    prefix = { Text(currency.symbol) },
                    isError = largeMinor == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.manage_new_subscriptions),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = subscriptions, onCheckedChange = { subscriptions = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(threshold.roundToInt(), largeMinor ?: alerts.largePaymentMinor, subscriptions) },
                enabled =
                    largeMinor != null,
            ) {
                Text(stringResource(Res.string.manage_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.manage_cancel)) } },
    )
}

/** Editor for the projection assumptions; ranges follow the design canvas's own controls. */
@Composable
internal fun AssumptionsDialog(
    assumptions: Assumptions,
    onSave: (Assumptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var rate by remember { mutableFloatStateOf(assumptions.returnRatePct.toFloat()) }
    var income by remember { mutableFloatStateOf(assumptions.incomeGrowthPct.toFloat()) }
    var prices by remember { mutableFloatStateOf(assumptions.priceRisePct.toFloat()) }
    var months by remember { mutableFloatStateOf(assumptions.emergencyFundMonths.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.manage_assumptions)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(Res.string.manage_assumptions_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AssumptionSlider(stringResource(Res.string.manage_assumption_return, percent(rate.toDouble())), rate, 0f..7f, 13) {
                    rate =
                        it
                }
                AssumptionSlider(stringResource(Res.string.manage_assumption_income, percent(income.toDouble())), income, 0f..6f, 11) {
                    income =
                        it
                }
                AssumptionSlider(stringResource(Res.string.manage_assumption_prices, percent(prices.toDouble())), prices, 0f..6f, 11) {
                    prices =
                        it
                }
                AssumptionSlider(
                    pluralStringResource(Res.plurals.manage_assumption_fund, months.roundToInt(), months.roundToInt()),
                    months,
                    1f..12f,
                    10,
                ) { months = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(Assumptions(halfSteps(rate), halfSteps(income), halfSteps(prices), months.roundToInt()))
            }) { Text(stringResource(Res.string.manage_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.manage_cancel)) } },
    )
}

@Composable
private fun AssumptionSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

/** Snaps a slider value to the 0.5 % steps the assumptions use. */
internal fun halfSteps(value: Float): Double = (value * 2).roundToInt() / 2.0

/** 3.0 → "3", 2.5 → "2.5". */
internal fun percent(value: Double): String {
    val snapped = (value * 2).roundToInt() / 2.0
    return if (snapped % 1.0 == 0.0) snapped.toLong().toString() else snapped.toString()
}
