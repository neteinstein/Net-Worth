package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.generated.resources.Res
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_sync_offline
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_sync_pending
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_sync_synced
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_sync_syncing
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.repository.SyncStatus
import org.jetbrains.compose.resources.stringResource

/** "Saved to Google Drive" / "Offline · changes kept on this device"; tap to sync now. */
@Composable
fun SyncIndicator(
    status: SyncStatus,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (status) {
        SyncStatus.SYNCED -> stringResource(Res.string.designsystem_sync_synced) to NetWorthTheme.financeColors.positive
        SyncStatus.SYNCING -> stringResource(Res.string.designsystem_sync_syncing) to MaterialTheme.colorScheme.primary
        SyncStatus.PENDING -> stringResource(Res.string.designsystem_sync_pending) to MaterialTheme.colorScheme.primary
        SyncStatus.OFFLINE -> stringResource(Res.string.designsystem_sync_offline) to NetWorthTheme.financeColors.negative
    }
    Row(
        modifier.heightIn(min = 32.dp).clickable(role = Role.Button, onClick = onSyncNow).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
