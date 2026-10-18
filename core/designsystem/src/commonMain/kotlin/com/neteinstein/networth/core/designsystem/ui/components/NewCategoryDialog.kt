package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.neteinstein.networth.core.designsystem.generated.resources.Res
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_add
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_cancel
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_new_category
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_new_category_name
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_new_subcategory
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import org.jetbrains.compose.resources.stringResource

/** "+ New category" / "+ New subcategory": the chip that ends a list of categories when none fits. */
@Composable
fun newCategoryLabel(subcategory: Boolean = false): String =
    "+ " + stringResource(if (subcategory) Res.string.designsystem_new_subcategory else Res.string.designsystem_new_category)

/**
 * Asks for the name of a new category (or [subcategory]) while filing a transaction; [onAdd] creates it and picks it.
 */
@Composable
fun NewCategoryDialog(
    onAdd: (name: String) -> Unit,
    onDismiss: () -> Unit,
    subcategory: Boolean = false,
) {
    var name by remember { mutableStateOf("") }
    val canAdd = name.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (subcategory) Res.string.designsystem_new_subcategory else Res.string.designsystem_new_category))
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.designsystem_new_category_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canAdd) onAdd(name) }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onAdd(name) }, enabled = canAdd) { Text(stringResource(Res.string.designsystem_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.designsystem_cancel)) } },
    )
}

@Preview
@Composable
private fun NewCategoryDialogPreview() {
    NetWorthTheme {
        Surface(color = MaterialTheme.colorScheme.background) { NewCategoryDialog(onAdd = {}, onDismiss = {}) }
    }
}
