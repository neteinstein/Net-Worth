package com.neteinstein.networth.feature.manage.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons

/** Round back button, big title and optional trailing actions — the artboards' screen header. */
@Composable
internal fun ScreenHeader(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedIconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(NetWorthIcons.ChevronLeft, contentDescription = backLabel, modifier = Modifier.size(20.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
        actions()
    }
}
