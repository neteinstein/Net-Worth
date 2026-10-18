package com.neteinstein.networth.feature.spending.ui.screens

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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.CardTitle
import com.neteinstein.networth.core.designsystem.ui.components.ChipRow
import com.neteinstein.networth.core.designsystem.ui.components.ChoiceChip
import com.neteinstein.networth.core.designsystem.ui.components.ColorKey
import com.neteinstein.networth.core.designsystem.ui.components.EmptyState
import com.neteinstein.networth.core.designsystem.ui.components.HeroCard
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.components.ProportionBar
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.components.SegmentedControl
import com.neteinstein.networth.core.designsystem.ui.components.monthNames
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthNames
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.designsystem.ui.theme.categoryColor
import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.feature.spending.domain.model.DateRange
import com.neteinstein.networth.feature.spending.domain.model.PeriodKind
import com.neteinstein.networth.feature.spending.domain.model.UNCATEGORISED
import com.neteinstein.networth.feature.spending.domain.usecase.CategoryRow
import com.neteinstein.networth.feature.spending.domain.usecase.SavingsFlow
import com.neteinstein.networth.feature.spending.domain.usecase.Spending
import com.neteinstein.networth.feature.spending.generated.resources.Res
import com.neteinstein.networth.feature.spending.generated.resources.spending_all_accounts
import com.neteinstein.networth.feature.spending.generated.resources.spending_budget_left
import com.neteinstein.networth.feature.spending.generated.resources.spending_budget_over
import com.neteinstein.networth.feature.spending.generated.resources.spending_cancel
import com.neteinstein.networth.feature.spending.generated.resources.spending_custom
import com.neteinstein.networth.feature.spending.generated.resources.spending_delta_down
import com.neteinstein.networth.feature.spending.generated.resources.spending_delta_flat
import com.neteinstein.networth.feature.spending.generated.resources.spending_delta_up
import com.neteinstein.networth.feature.spending.generated.resources.spending_empty_body
import com.neteinstein.networth.feature.spending.generated.resources.spending_empty_title
import com.neteinstein.networth.feature.spending.generated.resources.spending_filters
import com.neteinstein.networth.feature.spending.generated.resources.spending_filters_count
import com.neteinstein.networth.feature.spending.generated.resources.spending_kids_savings
import com.neteinstein.networth.feature.spending.generated.resources.spending_month
import com.neteinstein.networth.feature.spending.generated.resources.spending_next
import com.neteinstein.networth.feature.spending.generated.resources.spending_no_history
import com.neteinstein.networth.feature.spending.generated.resources.spending_no_savings_spending
import com.neteinstein.networth.feature.spending.generated.resources.spending_ok
import com.neteinstein.networth.feature.spending.generated.resources.spending_per_person
import com.neteinstein.networth.feature.spending.generated.resources.spending_previous
import com.neteinstein.networth.feature.spending.generated.resources.spending_savings_flow
import com.neteinstein.networth.feature.spending.generated.resources.spending_scope_period
import com.neteinstein.networth.feature.spending.generated.resources.spending_share_of_family
import com.neteinstein.networth.feature.spending.generated.resources.spending_spike
import com.neteinstein.networth.feature.spending.generated.resources.spending_title
import com.neteinstein.networth.feature.spending.generated.resources.spending_transfers_note
import com.neteinstein.networth.feature.spending.generated.resources.spending_uncategorised
import com.neteinstein.networth.feature.spending.generated.resources.spending_vs_average
import com.neteinstein.networth.feature.spending.generated.resources.spending_week
import com.neteinstein.networth.feature.spending.generated.resources.spending_year
import com.neteinstein.networth.feature.spending.ui.PeriodLabels
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpendingScreen(
    onOpenFilters: () -> Unit,
    onOpenCategory: (categoryId: String?, period: DateRange, accountIds: List<String>) -> Unit,
    viewModel: SpendingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SpendingContent(
        state = state,
        actions =
            SpendingActions(
                onKind = viewModel::onKindSelected,
                onCustomRange = viewModel::onCustomRange,
                onPrevious = viewModel::onPrevious,
                onNext = viewModel::onNext,
                onScope = viewModel::onScopeSelected,
                onOpenFilters = onOpenFilters,
                onOpenCategory = onOpenCategory,
            ),
    )
}

data class SpendingActions(
    val onKind: (PeriodKind) -> Unit = {},
    val onCustomRange: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onScope: (AccountScope) -> Unit = {},
    val onOpenFilters: () -> Unit = {},
    val onOpenCategory: (String?, DateRange, List<String>) -> Unit = { _, _, _ -> },
)

@Composable
fun SpendingContent(
    state: SpendingUiState,
    actions: SpendingActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        val spending = state.spending
        if (spending == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val currency = CurrencyCode.of(spending.currencyCode)
        var pickingRange by remember { mutableStateOf(false) }
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
                val cards = Modifier.padding(horizontal = 20.dp)
                item { Header(spending, actions, cards) }
                item {
                    val kinds = PeriodKind.entries
                    SegmentedControl(
                        options = kinds,
                        selected = spending.kind,
                        label = { kindLabel(it) },
                        onSelect = { kind -> if (kind == PeriodKind.CUSTOM) pickingRange = true else actions.onKind(kind) },
                        modifier = cards,
                    )
                }
                item {
                    ChipRow {
                        val filters =
                            if (state.activeFilters >
                                0
                            ) {
                                stringResource(Res.string.spending_filters_count, state.activeFilters)
                            } else {
                                stringResource(Res.string.spending_filters)
                            }
                        ChoiceChip(filters, selected = false, onClick = actions.onOpenFilters, leading = {
                            Icon(NetWorthIcons.Sliders, contentDescription = null, modifier = Modifier.size(16.dp))
                        })
                        spending.scopes.forEach { scope ->
                            ChoiceChip(scopeLabel(scope, spending), selected = scope == spending.scope, onClick = {
                                actions.onScope(scope)
                            })
                        }
                    }
                }
                item { TotalCard(spending, currency, cards) }
                val savings = spending.savings
                if (savings != null) {
                    item { SavingsCard(spending, savings, currency, cards) }
                } else if (spending.rows.isEmpty()) {
                    item {
                        EmptyState(stringResource(Res.string.spending_empty_title), stringResource(Res.string.spending_empty_body), cards)
                    }
                } else {
                    item {
                        CategoriesCard(spending, currency, cards) { row ->
                            actions.onOpenCategory(row.categoryId ?: UNCATEGORISED, spending.period, spending.scopeAccountIds)
                        }
                    }
                }
                item {
                    Text(
                        stringResource(Res.string.spending_transfers_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = cards,
                    )
                }
            }
        }
        if (pickingRange) {
            RangePickerDialog(
                current = spending.period,
                onPicked = { start, end ->
                    pickingRange = false
                    actions.onCustomRange(start, end)
                },
                onDismiss = { pickingRange = false },
            )
        }
    }
}

@Composable
private fun Header(
    spending: Spending,
    actions: SpendingActions,
    modifier: Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.spending_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        IconButton(onClick = actions.onPrevious) {
            Icon(
                NetWorthIcons.ChevronLeft,
                contentDescription = stringResource(Res.string.spending_previous),
                modifier = Modifier.size(20.dp),
            )
        }
        Text(PeriodLabels.title(spending.kind, spending.period, shortMonthNames()), style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = actions.onNext) {
            Icon(NetWorthIcons.ChevronRight, contentDescription = stringResource(Res.string.spending_next), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun kindLabel(kind: PeriodKind): String =
    stringResource(
        when (kind) {
            PeriodKind.WEEK -> Res.string.spending_week
            PeriodKind.MONTH -> Res.string.spending_month
            PeriodKind.YEAR -> Res.string.spending_year
            PeriodKind.CUSTOM -> Res.string.spending_custom
        },
    )

@Composable
private fun scopeLabel(
    scope: AccountScope,
    spending: Spending,
): String =
    when (scope) {
        AccountScope.All -> stringResource(Res.string.spending_all_accounts)
        AccountScope.Children -> stringResource(Res.string.spending_kids_savings)
        is AccountScope.Single -> if (scope ==
            spending.scope
        ) {
            spending.scopeName.orEmpty()
        } else {
            spending.scopeNames[scope.accountId].orEmpty()
        }
    }

@Composable
private fun TotalCard(
    spending: Spending,
    currency: CurrencyCode,
    modifier: Modifier,
) {
    val finance = NetWorthTheme.financeColors
    HeroCard(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val period = PeriodLabels.inline(spending.kind, spending.period, shortMonthNames(), monthNames())
                Text(
                    stringResource(Res.string.spending_scope_period, scopeLabel(spending.scope, spending), period),
                    style = MaterialTheme.typography.labelMedium,
                    color = finance.heroMuted,
                )
                Text(
                    MoneyFormatter.whole(spending.totalMinor, currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = finance.onHeroContainer,
                )
            }
            val perPerson = spending.perPersonMinor
            val share = spending.shareOfFamilyPercent
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (perPerson != null) Res.string.spending_per_person else Res.string.spending_share_of_family),
                    style = MaterialTheme.typography.labelMedium,
                    color = finance.heroMuted,
                )
                Text(
                    when {
                        perPerson != null -> MoneyFormatter.whole(perPerson, currency)
                        share != null -> "$share%"
                        else -> "—"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = finance.onHeroContainer,
                )
            }
        }
    }
}

@Composable
private fun CategoriesCard(
    spending: Spending,
    currency: CurrencyCode,
    modifier: Modifier,
    onRow: (CategoryRow) -> Unit,
) {
    val finance = NetWorthTheme.financeColors
    SectionCard(modifier, spacing = 12) {
        CardTitle(stringResource(Res.string.spending_vs_average))
        val max = spending.maxRowMinor.coerceAtLeast(1)
        spending.rows.forEach { row ->
            val color = row.colorHex?.let { categoryColor(it) } ?: finance.chartMuted
            Column(
                Modifier.fillMaxWidth().clickable { onRow(row) }.heightIn(min = 44.dp).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // As on the artboard: name, amount and change on one line; the bar spans the row below.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorKey(color)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        row.name ?: stringResource(Res.string.spending_uncategorised),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(MoneyFormatter.whole(row.amountMinor, currency), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(10.dp))
                    val delta = row.deltaPercent
                    val text =
                        when {
                            delta == null -> stringResource(Res.string.spending_no_history)
                            delta > 0 -> stringResource(Res.string.spending_delta_up, delta)
                            delta < 0 -> stringResource(Res.string.spending_delta_down, -delta)
                            else -> stringResource(Res.string.spending_delta_flat)
                        }
                    Text(
                        if (row.isSpike) stringResource(Res.string.spending_spike, text) else text,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (row.isSpike) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        color =
                            when {
                                row.isSpike -> finance.negative
                                (delta ?: 0) < 0 -> finance.positive
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        maxLines = 1,
                        // Fixed, as on the artboard, so the amounts line up in a column.
                        modifier = Modifier.width(104.dp),
                    )
                }
                ProportionBar(row.amountMinor.toFloat() / max, color, Modifier.padding(start = 18.dp))
                row.budgetMinor?.let { budget ->
                    val over = row.overBudgetMinor ?: 0L
                    Text(
                        if (over > 0) {
                            stringResource(
                                Res.string.spending_budget_over,
                                MoneyFormatter.whole(over, currency),
                                MoneyFormatter.whole(budget, currency),
                            )
                        } else {
                            stringResource(
                                Res.string.spending_budget_left,
                                MoneyFormatter.whole(-over, currency),
                                MoneyFormatter.whole(budget, currency),
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (over > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (over > 0) finance.negative else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsCard(
    spending: Spending,
    savings: SavingsFlow,
    currency: CurrencyCode,
    modifier: Modifier,
) {
    SectionCard(modifier, spacing = 6) {
        Text(stringResource(Res.string.spending_no_savings_spending), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(
                Res.string.spending_savings_flow,
                PeriodLabels.inline(spending.kind, spending.period, shortMonthNames(), monthNames()),
                scopeLabel(spending.scope, spending),
                MoneyFormatter.format(savings.inMinor, currency),
                MoneyFormatter.format(savings.outMinor, currency),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(
    current: DateRange,
    onPicked: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val picker =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = current.start.toEpochDays() * MILLIS_PER_DAY,
            initialSelectedEndDateMillis = current.end.toEpochDays() * MILLIS_PER_DAY,
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val start = picker.selectedStartDateMillis
            val end = picker.selectedEndDateMillis
            TextButton(
                onClick = { if (start != null && end != null) onPicked(start.toLocalDate(), end.toLocalDate()) },
                enabled = start != null && end != null,
            ) { Text(stringResource(Res.string.spending_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.spending_cancel)) } },
    ) {
        DateRangePicker(state = picker, modifier = Modifier.weight(1f))
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private fun Long.toLocalDate(): LocalDate = LocalDate.fromEpochDays(this / MILLIS_PER_DAY)

@Preview
@Composable
private fun SpendingPreview() {
    NetWorthTheme { SpendingContent(previewSpendingState(), SpendingActions()) }
}

@Preview
@Composable
private fun SpendingSavingsDarkPreview() {
    NetWorthTheme(darkTheme = true) {
        val state = previewSpendingState()
        SpendingContent(
            state.copy(
                spending = state.spending!!.copy(scope = AccountScope.Children, rows = emptyList(), savings = SavingsFlow(9_000, 0)),
            ),
            SpendingActions(),
        )
    }
}
