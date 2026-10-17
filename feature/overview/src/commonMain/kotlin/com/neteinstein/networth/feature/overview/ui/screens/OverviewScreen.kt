package com.neteinstein.networth.feature.overview.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.ChipRow
import com.neteinstein.networth.core.designsystem.ui.components.ChoiceChip
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.components.SyncIndicator
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthYear
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.feature.overview.domain.usecase.Overview
import com.neteinstein.networth.feature.overview.generated.resources.Res
import com.neteinstein.networth.feature.overview.generated.resources.overview_change_period
import com.neteinstein.networth.feature.overview.generated.resources.overview_family
import com.neteinstein.networth.feature.overview.generated.resources.overview_manage
import com.neteinstein.networth.feature.overview.generated.resources.overview_scope_all
import com.neteinstein.networth.feature.overview.generated.resources.overview_scope_children
import com.neteinstein.networth.feature.overview.generated.resources.overview_title
import com.neteinstein.networth.feature.overview.ui.components.EmptyHistoryCard
import com.neteinstein.networth.feature.overview.ui.components.NetWorthCard
import com.neteinstein.networth.feature.overview.ui.components.SavedHeroCard
import com.neteinstein.networth.feature.overview.ui.components.SpikeCard
import com.neteinstein.networth.feature.overview.ui.components.TrendCard
import com.neteinstein.networth.feature.overview.ui.components.WhereItWentCard
import com.neteinstein.networth.feature.overview.ui.components.previewOverview
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OverviewScreen(
    onOpenManage: () -> Unit,
    viewModel: OverviewViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OverviewContent(
        state = state,
        onMonthSelected = viewModel::onMonthSelected,
        onScopeSelected = viewModel::onScopeSelected,
        onMarkSeasonal = viewModel::onMarkSeasonal,
        onSyncNow = viewModel::onSyncNow,
        onOpenManage = onOpenManage,
    )
}

@Composable
fun OverviewContent(
    state: OverviewUiState,
    onMonthSelected: (CalendarMonth) -> Unit,
    onScopeSelected: (AccountScope) -> Unit,
    onMarkSeasonal: (String) -> Unit,
    onSyncNow: () -> Unit,
    onOpenManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        val overview = state.overview
        if (overview == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val currency = CurrencyCode.of(overview.currencyCode)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom =
                        padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Header(overview, state.sync, onMonthSelected, onSyncNow, onOpenManage) }
                if (overview.scopes.size > 1) {
                    item {
                        ChipRow {
                            overview.scopes.forEach { scope ->
                                ChoiceChip(scopeLabel(scope, overview), selected = scope == overview.scope, onClick = {
                                    onScopeSelected(scope)
                                })
                            }
                        }
                    }
                }
                val cards = Modifier.padding(horizontal = 20.dp)
                if (overview.hasTransactions) {
                    item { SavedHeroCard(overview, currency, cards) }
                    overview.spike?.let { spike -> item { SpikeCard(spike, currency, { onMarkSeasonal(spike.categoryId) }, cards) } }
                    item { WhereItWentCard(overview, currency, cards) }
                    item { TrendCard(overview, currency, cards) }
                } else {
                    item { EmptyHistoryCard(cards) }
                }
                item { NetWorthCard(overview, currency, cards) }
            }
        }
    }
}

@Composable
private fun Header(
    overview: Overview,
    sync: SyncStatus?,
    onMonthSelected: (CalendarMonth) -> Unit,
    onSyncNow: () -> Unit,
    onOpenManage: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(Res.string.overview_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics {
                        heading()
                    },
                )
                val people = overview.adults + overview.children
                Text(
                    pluralStringResource(Res.plurals.overview_family, people, overview.householdName, people),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MonthPicker(overview.month, overview.months, onMonthSelected)
            Spacer(Modifier.size(6.dp))
            OutlinedIconButton(onClick = onOpenManage, modifier = Modifier.size(44.dp)) {
                Icon(
                    NetWorthIcons.Sliders,
                    contentDescription = stringResource(Res.string.overview_manage),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (sync != null) SyncIndicator(sync, onSyncNow)
    }
}

@Composable
private fun MonthPicker(selected: CalendarMonth, months: List<CalendarMonth>, onSelected: (CalendarMonth) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = shortMonthYear(selected.month, selected.year)
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier.heightIn(min = 44.dp).semantics { },
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(
                NetWorthIcons.ChevronDown,
                contentDescription = stringResource(Res.string.overview_change_period, label),
                modifier = Modifier.padding(start = 6.dp).size(16.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = { Text(shortMonthYear(month.month, month.year)) },
                    onClick = {
                        expanded = false
                        onSelected(month)
                    },
                )
            }
        }
    }
}

@Composable
private fun scopeLabel(scope: AccountScope, overview: Overview): String = when (scope) {
    AccountScope.All -> stringResource(Res.string.overview_scope_all)
    AccountScope.Children -> stringResource(Res.string.overview_scope_children)
    is AccountScope.Single -> overview.accounts.firstOrNull { it.id == scope.accountId }?.name.orEmpty()
}

@Preview
@Composable
private fun OverviewPreview() {
    NetWorthTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            OverviewContent(OverviewUiState(previewOverview(), SyncStatus.SYNCED), {}, {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
private fun OverviewEmptyDarkPreview() {
    NetWorthTheme(darkTheme = true) {
        OverviewContent(OverviewUiState(previewOverview().copy(hasTransactions = false), SyncStatus.OFFLINE), {}, {}, {}, {}, {})
    }
}
