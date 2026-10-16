package com.neteinstein.networth.feature.dashboard.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.AppearAnimation
import com.neteinstein.networth.core.designsystem.ui.components.SectionHeader
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.feature.dashboard.data.SampleAccounts
import com.neteinstein.networth.feature.dashboard.domain.model.Account
import com.neteinstein.networth.feature.dashboard.domain.model.NetWorthSummary
import com.neteinstein.networth.feature.dashboard.generated.resources.Res
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_assets
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_empty_body
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_empty_title
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_liabilities
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_open_settings
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_title
import com.neteinstein.networth.feature.dashboard.ui.components.AccountRow
import com.neteinstein.networth.feature.dashboard.ui.components.NetWorthHeroCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Screen entry point: wires the ViewModel to the stateless [DashboardContent]. */
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardContent(state = state, onOpenSettings = onOpenSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(Res.string.dashboard_open_settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = when {
                state.isLoading -> Phase.Loading
                state.isEmpty -> Phase.Empty
                else -> Phase.Content
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "dashboard-phase",
        ) { phase ->
            when (phase) {
                Phase.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }
                Phase.Empty -> EmptyState(Modifier.padding(padding))
                Phase.Content -> SummaryList(state, padding)
            }
        }
    }
}

private enum class Phase { Loading, Empty, Content }

@Composable
private fun SummaryList(state: DashboardUiState, padding: PaddingValues) {
    val summary = state.summary
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 32.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "hero") {
            AppearAnimation(modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH)) {
                NetWorthHeroCard(summary)
            }
        }
        accountSection("assets", Res.string.dashboard_assets, summary.assets, offset = 1)
        accountSection("liabilities", Res.string.dashboard_liabilities, summary.liabilities, offset = 1 + summary.assets.size)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.accountSection(
    key: String,
    title: org.jetbrains.compose.resources.StringResource,
    accounts: List<Account>,
    offset: Int,
) {
    if (accounts.isEmpty()) return
    item(key = "header-$key") {
        SectionHeader(stringResource(title), Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth())
    }
    itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
        AppearAnimation(index = offset + index, modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH)) {
            AccountRow(account, Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.dashboard_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.dashboard_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val MAX_CONTENT_WIDTH = 640.dp

@Preview
@Composable
private fun DashboardContentPreview() {
    NetWorthTheme {
        DashboardContent(
            state = DashboardUiState(
                isLoading = false,
                summary = NetWorthSummary.from(SampleAccounts.all),
            ),
            onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun DashboardEmptyPreview() {
    NetWorthTheme(darkTheme = true) {
        DashboardContent(state = DashboardUiState(isLoading = false), onOpenSettings = {})
    }
}
