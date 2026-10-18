package com.neteinstein.networth.feature.future.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.CardTitle
import com.neteinstein.networth.core.designsystem.ui.components.ChipRow
import com.neteinstein.networth.core.designsystem.ui.components.ChoiceChip
import com.neteinstein.networth.core.designsystem.ui.components.EmptyState
import com.neteinstein.networth.core.designsystem.ui.components.HeroCard
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.components.ProportionBar
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.components.SegmentedControl
import com.neteinstein.networth.core.designsystem.ui.components.monthName
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthName
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthYear
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.feature.future.domain.model.EmergencyFund
import com.neteinstein.networth.feature.future.domain.model.Future
import com.neteinstein.networth.feature.future.domain.model.FutureScope
import com.neteinstein.networth.feature.future.generated.resources.Res
import com.neteinstein.networth.feature.future.generated.resources.future_and
import com.neteinstein.networth.feature.future.generated.resources.future_change_assumptions
import com.neteinstein.networth.feature.future.generated.resources.future_chart_description
import com.neteinstein.networth.feature.future.generated.resources.future_chart_now
import com.neteinstein.networth.feature.future.generated.resources.future_chart_year
import com.neteinstein.networth.feature.future.generated.resources.future_cut_dining
import com.neteinstein.networth.feature.future.generated.resources.future_cut_dining_body
import com.neteinstein.networth.feature.future.generated.resources.future_disclaimer
import com.neteinstein.networth.feature.future.generated.resources.future_empty_body
import com.neteinstein.networth.feature.future.generated.resources.future_empty_title
import com.neteinstein.networth.feature.future.generated.resources.future_extra
import com.neteinstein.networth.feature.future.generated.resources.future_extra_body
import com.neteinstein.networth.feature.future.generated.resources.future_fund_covered
import com.neteinstein.networth.feature.future.generated.resources.future_fund_months
import com.neteinstein.networth.feature.future.generated.resources.future_fund_never
import com.neteinstein.networth.feature.future.generated.resources.future_fund_progress
import com.neteinstein.networth.feature.future.generated.resources.future_fund_reached
import com.neteinstein.networth.feature.future.generated.resources.future_fund_title
import com.neteinstein.networth.feature.future.generated.resources.future_if_earn
import com.neteinstein.networth.feature.future.generated.resources.future_kid_now
import com.neteinstein.networth.feature.future.generated.resources.future_legend_flat
import com.neteinstein.networth.feature.future.generated.resources.future_legend_return
import com.neteinstein.networth.feature.future.generated.resources.future_scope_all
import com.neteinstein.networth.feature.future.generated.resources.future_scope_joint
import com.neteinstein.networth.feature.future.generated.resources.future_scope_kids
import com.neteinstein.networth.feature.future.generated.resources.future_set_aside
import com.neteinstein.networth.feature.future.generated.resources.future_set_aside_body
import com.neteinstein.networth.feature.future.generated.resources.future_spend_prices
import com.neteinstein.networth.feature.future.generated.resources.future_spend_title
import com.neteinstein.networth.feature.future.generated.resources.future_subtitle
import com.neteinstein.networth.feature.future.generated.resources.future_title
import com.neteinstein.networth.feature.future.generated.resources.future_what_if
import com.neteinstein.networth.feature.future.generated.resources.future_when
import com.neteinstein.networth.feature.future.generated.resources.future_year_ahead
import com.neteinstein.networth.feature.future.generated.resources.future_year_ahead_peaks
import com.neteinstein.networth.feature.future.generated.resources.future_year_ahead_plain
import com.neteinstein.networth.feature.future.generated.resources.future_years
import com.neteinstein.networth.feature.future.ui.FutureFormat
import com.neteinstein.networth.feature.future.ui.components.ProjectionChart
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FutureScreen(
    onOpenYearAhead: () -> Unit,
    onChangeAssumptions: () -> Unit,
    viewModel: FutureViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FutureContent(
        state = state,
        onScopeSelected = viewModel::onScopeSelected,
        onHorizonSelected = viewModel::onHorizonSelected,
        onToggleCutDining = viewModel::onToggleCutDining,
        onToggleExtra = viewModel::onToggleExtra,
        onToggleSetAside = viewModel::onToggleSetAside,
        onOpenYearAhead = onOpenYearAhead,
        onChangeAssumptions = onChangeAssumptions,
    )
}

@Composable
fun FutureContent(
    state: FutureUiState,
    onScopeSelected: (FutureScope) -> Unit,
    onHorizonSelected: (Int) -> Unit,
    onToggleCutDining: () -> Unit,
    onToggleExtra: () -> Unit,
    onToggleSetAside: () -> Unit,
    onOpenYearAhead: () -> Unit,
    modifier: Modifier = Modifier,
    onChangeAssumptions: () -> Unit = {},
) {
    Scaffold(modifier = modifier) { padding ->
        val future = state.future
        if (future == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val currency = CurrencyCode.of(future.currencyCode)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom =
                        padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val cards = Modifier.padding(horizontal = 20.dp)
                item {
                    Column(cards, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(Res.string.future_title),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                        if (future.latest != null) {
                            Text(
                                stringResource(Res.string.future_subtitle, MoneyFormatter.whole(future.monthlyMinor, currency)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                val latest = future.latest
                if (latest == null) {
                    item { EmptyState(stringResource(Res.string.future_empty_title), stringResource(Res.string.future_empty_body), cards) }
                    return@LazyColumn
                }
                if (future.scopes.size > 1) {
                    item {
                        ChipRow {
                            future.scopes.forEach { scope ->
                                ChoiceChip(scopeLabel(scope), selected = scope == future.scope, onClick = { onScopeSelected(scope) })
                            }
                        }
                    }
                }
                item { ProjectionCard(future, state.horizon, latest.year, currency, onHorizonSelected, cards) }
                item { YearAheadLink(state, onOpenYearAhead, cards) }
                item { WhatIfCard(future, currency, onToggleCutDining, onToggleExtra, onToggleSetAside, cards) }
                future.emergencyFund?.let { fund -> item { EmergencyFundCard(fund, currency, cards) } }
                item { TenYearSpendCard(future, currency, cards) }
                item {
                    Column(cards) {
                        Text(
                            stringResource(Res.string.future_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onChangeAssumptions, contentPadding = PaddingValues(0.dp)) {
                            Text(stringResource(Res.string.future_change_assumptions), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun scopeLabel(scope: FutureScope): String = stringResource(
    when (scope) {
        FutureScope.ALL -> Res.string.future_scope_all
        FutureScope.JOINT -> Res.string.future_scope_joint
        FutureScope.KIDS -> Res.string.future_scope_kids
    },
)

@Composable
private fun ProjectionCard(
    future: Future,
    horizon: Int,
    year: Int,
    currency: CurrencyCode,
    onHorizonSelected: (Int) -> Unit,
    modifier: Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val fraction = currency.fractionDigits
    val rate = FutureFormat.rate(future.returnRatePct)
    val flat = future.flat.getOrElse(horizon) { 0L }
    val withReturn = future.withReturn.getOrElse(horizon) { 0L }
    SectionCard(modifier) {
        SegmentedControl(
            options = FutureUiState.HORIZONS,
            selected = horizon,
            label = { stringResource(Res.string.future_years, it) },
            onSelect = onHorizonSelected,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(Res.string.future_when, horizon, year + horizon),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(MoneyFormatter.whole(FutureFormat.roundTo(flat, 10, fraction), currency), style = MaterialTheme.typography.displayLarge)
            val amount = MoneyFormatter.whole(FutureFormat.roundTo(withReturn, 10, fraction), currency)
            val line = stringResource(Res.string.future_if_earn, amount, rate)
            Text(
                buildAnnotatedString {
                    val start = line.indexOf(amount)
                    if (start < 0) {
                        append(line)
                    } else {
                        append(line.substring(0, start))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accent)) { append(amount) }
                        append(line.substring(start + amount.length))
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val max = FutureFormat.chartMax(future.withReturn.maxOrNull() ?: 0L, fraction)
        ProjectionChart(
            flat = future.flat,
            withReturn = future.withReturn,
            horizon = horizon,
            max = max,
            topLabel = FutureFormat.thousands(max, currency),
            midLabel = FutureFormat.thousands(max / 2, currency),
            xLabels = listOf(0 to stringResource(Res.string.future_chart_now)) +
                listOf(2, 5, 10).map { it to stringResource(Res.string.future_chart_year, it) },
            description = stringResource(Res.string.future_chart_description),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(accent, solid = true, stringResource(Res.string.future_legend_return, rate))
            LegendItem(MaterialTheme.colorScheme.onSurfaceVariant, solid = false, stringResource(Res.string.future_legend_flat))
        }
        if (future.scope == FutureScope.KIDS && future.kids.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
            future.kids.forEach { kid ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(Res.string.future_kid_now, kid.name, MoneyFormatter.whole(kid.nowMinor, currency)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        MoneyFormatter.whole(FutureFormat.roundTo(kid.withReturn.getOrElse(horizon) { 0L }, 10, fraction), currency),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, solid: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (solid) {
            ProportionBar(1f, color, Modifier.width(16.dp), height = 3.dp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { ProportionBar(1f, color, Modifier.width(4.dp), height = 2.dp) }
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun YearAheadLink(state: FutureUiState, onOpen: () -> Unit, modifier: Modifier) {
    val finance = NetWorthTheme.financeColors
    val and = stringResource(Res.string.future_and, "{a}", "{b}")
    val months = state.peakMonths.map { shortMonthName(it.month) }
    val subtitle = if (months.isEmpty()) {
        stringResource(Res.string.future_year_ahead_plain)
    } else {
        stringResource(
            Res.string.future_year_ahead_peaks,
            FutureFormat.joinWithAnd(months) { a, b -> and.replace("{a}", a).replace("{b}", b) },
        )
    }
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onOpen),
        shape = MaterialTheme.shapes.medium,
        color = finance.heroContainer,
        contentColor = finance.onHeroContainer,
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.future_year_ahead), style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = finance.heroMuted)
            }
            Spacer(Modifier.width(12.dp))
            Icon(NetWorthIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun WhatIfCard(
    future: Future,
    currency: CurrencyCode,
    onToggleCutDining: () -> Unit,
    onToggleExtra: () -> Unit,
    onToggleSetAside: () -> Unit,
    modifier: Modifier,
) {
    SectionCard(modifier, spacing = 0) {
        CardTitle(stringResource(Res.string.future_what_if), Modifier.padding(bottom = 6.dp))
        var first = true

        @Composable
        fun option(checked: Boolean, title: String, body: String, onToggle: () -> Unit) {
            if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
            first = false
            WhatIfRow(checked, title, body, onToggle)
        }
        future.diningCutMinor?.let {
            option(
                future.whatIfs.cutDining,
                stringResource(Res.string.future_cut_dining),
                stringResource(Res.string.future_cut_dining_body, MoneyFormatter.whole(it, currency)),
                onToggleCutDining,
            )
        }
        option(
            future.whatIfs.addExtra,
            stringResource(Res.string.future_extra, MoneyFormatter.whole(future.extraMinor, currency)),
            stringResource(Res.string.future_extra_body),
            onToggleExtra,
        )
        future.setAside?.let {
            option(
                future.whatIfs.setAside,
                stringResource(Res.string.future_set_aside, MoneyFormatter.whole(it.monthlyMinor, currency), monthName(it.month)),
                stringResource(Res.string.future_set_aside_body, it.categoryName),
                onToggleSetAside,
            )
        }
    }
}

@Composable
private fun WhatIfRow(checked: Boolean, title: String, body: String, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmergencyFundCard(fund: EmergencyFund, currency: CurrencyCode, modifier: Modifier) {
    SectionCard(modifier, spacing = 10) {
        CardTitle(
            stringResource(Res.string.future_fund_title),
            trailing = pluralStringResource(Res.plurals.future_fund_months, fund.months, fund.months),
        )
        ProportionBar(fund.fraction, NetWorthTheme.financeColors.positive, height = 10.dp)
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    Res.string.future_fund_progress,
                    MoneyFormatter.whole(fund.currentMinor, currency),
                    MoneyFormatter.whole(fund.targetMinor, currency),
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            val reached = fund.reachedOn
            Text(
                when {
                    fund.covered -> stringResource(Res.string.future_fund_covered)
                    reached != null -> stringResource(Res.string.future_fund_reached, shortMonthYear(reached.month, reached.year))
                    else -> stringResource(Res.string.future_fund_never)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun TenYearSpendCard(future: Future, currency: CurrencyCode, modifier: Modifier) {
    val finance = NetWorthTheme.financeColors
    HeroCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.future_spend_title), style = MaterialTheme.typography.bodySmall, color = finance.heroMuted)
            Text(MoneyFormatter.whole(future.tenYearSpendMinor, currency), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(
                    Res.string.future_spend_prices,
                    MoneyFormatter.whole(
                        FutureFormat.roundTo(future.tenYearSpendWithPricesMinor, 1_000, currency.fractionDigits),
                        currency,
                    ),
                    FutureFormat.rate(future.priceRisePct),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
private fun FuturePreview() {
    NetWorthTheme {
        FutureContent(previewFutureState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
private fun FutureKidsDarkPreview() {
    NetWorthTheme(darkTheme = true) {
        val state = previewFutureState(FutureScope.KIDS)
        FutureContent(state, {}, {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
private fun FutureEmptyPreview() {
    NetWorthTheme {
        FutureContent(FutureUiState(future = previewFutureState().future!!.copy(latest = null)), {}, {}, {}, {}, {}, {})
    }
}
