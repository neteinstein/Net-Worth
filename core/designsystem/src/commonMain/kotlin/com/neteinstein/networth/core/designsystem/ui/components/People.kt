package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.generated.resources.Res
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_add
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_add_person
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_cancel
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_person_adult
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_person_child
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_person_name
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import org.jetbrains.compose.resources.stringResource

/**
 * Picks who an account belongs to: any number of household members (joint accounts have two),
 * plus an "Add person" chip for someone who isn't in the household yet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerPicker(
    members: List<Member>,
    selectedIds: List<String>,
    onToggle: (memberId: String) -> Unit,
    onAddPerson: (name: String, role: MemberRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    var adding by remember { mutableStateOf(false) }
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        members.forEach { member ->
            ChoiceChip(member.name, selected = member.id in selectedIds, onClick = { onToggle(member.id) })
        }
        ChoiceChip("+ " + stringResource(Res.string.designsystem_add_person), selected = false, onClick = { adding = true })
    }
    if (adding) {
        AddPersonDialog(
            onAdd = { name, role ->
                adding = false
                onAddPerson(name, role)
            },
            onDismiss = { adding = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPersonDialog(
    onAdd: (name: String, role: MemberRole) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(MemberRole.ADULT) }
    val canAdd = name.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.designsystem_add_person)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.designsystem_person_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canAdd) onAdd(name, role) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(stringResource(Res.string.designsystem_person_adult), role == MemberRole.ADULT, { role = MemberRole.ADULT })
                    ChoiceChip(stringResource(Res.string.designsystem_person_child), role == MemberRole.CHILD, { role = MemberRole.CHILD })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, role) }, enabled = canAdd) { Text(stringResource(Res.string.designsystem_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.designsystem_cancel)) } },
    )
}

@Preview
@Composable
private fun OwnerPickerPreview() {
    NetWorthTheme {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
            OwnerPicker(
                members = listOf(
                    Member("p", "Pedro", MemberRole.ADULT),
                    Member("m", "Maria", MemberRole.ADULT),
                    Member("i", "Inês", MemberRole.CHILD),
                ),
                selectedIds = listOf("p", "m"),
                onToggle = {},
                onAddPerson = { _, _ -> },
            )
        }
    }
}
