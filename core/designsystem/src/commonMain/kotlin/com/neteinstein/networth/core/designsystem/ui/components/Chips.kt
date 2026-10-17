package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme

/** Pill chip: ink when selected, outlined otherwise (account chips, filters). */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val finance = NetWorthTheme.financeColors
    Surface(
        modifier = modifier
            .heightIn(min = 40.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        shape = CircleShape,
        color = if (selected) finance.selectedChip else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) finance.onSelectedChip else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) finance.selectedChip else MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leading?.invoke()
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A horizontally scrolling row of chips that bleeds to the screen edges like on the artboards. */
@Composable
fun ChipRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
