package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate

/** A Material date range picker in a dialog, starting on [start]–[end]; [onPicked] gets both ends once chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    start: LocalDate?,
    end: LocalDate?,
    confirmLabel: String,
    dismissLabel: String,
    onPicked: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val picker =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = start?.let { it.toEpochDays() * MILLIS_PER_DAY },
            initialSelectedEndDateMillis = end?.let { it.toEpochDays() * MILLIS_PER_DAY },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val from = picker.selectedStartDateMillis
            val to = picker.selectedEndDateMillis
            TextButton(
                onClick = { if (from != null && to != null) onPicked(from.toLocalDate(), to.toLocalDate()) },
                enabled = from != null && to != null,
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    ) {
        DateRangePicker(state = picker, modifier = Modifier.weight(1f))
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private fun Long.toLocalDate(): LocalDate = LocalDate.fromEpochDays(this / MILLIS_PER_DAY)
