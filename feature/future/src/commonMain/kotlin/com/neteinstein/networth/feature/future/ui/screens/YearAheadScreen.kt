package com.neteinstein.networth.feature.future.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthName
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthYear
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.feature.future.domain.model.Forecast
import com.neteinstein.networth.feature.future.domain.model.Trend
import com.neteinstein.networth.feature.future.generated.resources.Res
import com.neteinstein.networth.feature.future.generated.resources.forecast_all_accounts
import com.neteinstein.networth.feature.future.generated.resources.forecast_back
import com.neteinstein.networth.feature.future.generated.resources.forecast_chart_description
import com.neteinstein.networth.feature.future.generated.resources.forecast_down_from
import com.neteinstein.networth.feature.future.generated.resources.forecast_income
import com.neteinstein.networth.feature.future.generated.resources.forecast_kids_savings
import com.neteinstein.networth.feature.future.generated.resources.forecast_month_by_month
import com.neteinstein.networth.feature.future.generated.resources.forecast_over_income
import com.neteinstein.networth.feature.future.generated.resources.forecast_peak_other
import com.neteinstein.networth.feature.future.generated.resources.forecast_peaks
import com.neteinstein.networth.feature.future.generated.resources.forecast_predicted_savings
import com.neteinstein.networth.feature.future.generated.resources.forecast_predicted_spending
import com.neteinstein.networth.feature.future.generated.resources.forecast_save
import com.neteinstein.networth.feature.future.generated.resources.forecast_savings
import com.neteinstein.networth.feature.future.generated.resources.forecast_set_aside
import com.neteinstein.networth.feature.future.generated.resources.forecast_share
import com.neteinstein.networth.feature.future.generated.resources.forecast_share_note
import com.neteinstein.networth.feature.future.generated.resources.forecast_spend
import com.neteinstein.networth.feature.future.generated.resources.forecast_subtitle
import com.neteinstein.networth.feature.future.generated.resources.forecast_title
import com.neteinstein.networth.feature.future.generated.resources.forecast_trend_flat
import com.neteinstein.networth.feature.future.generated.resources.forecast_trend_growing
import com.neteinstein.networth.feature.future.generated.resources.forecast_trend_shrinking
import com.neteinstein.networth.feature.future.generated.resources.forecast_typical
import com.neteinstein.networth.feature.future.generated.resources.forecast_up_from
import com.neteinstein.networth.feature.future.generated.resources.forecast_vs_last_down
import com.neteinstein.networth.feature.future.generated.resources.forecast_vs_last_up
import com.neteinstein.networth.feature.future.generated.resources.forecast_year
import com.neteinstein.networth.feature.future.generated.resources.forecast_year_by_year
import com.neteinstein.networth.feature.future.generated.resources.future_and
import com.neteinstein.networth.feature.future.generated.resources.future_empty_body
import com.neteinstein.networth.feature.future.generated.resources.future_empty_title
import com.neteinstein.networth.feature.future.ui.FutureFormat
import com.neteinstein.networth.feature.future.ui.components.ForecastBar
import com.neteinstein.networth.feature.future.ui.components.ForecastBars
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun YearAheadScreen(
    onBack: () -> Unit,
    viewModel: YearAheadViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    YearAheadContent(state, onBack, viewModel::onScopeSelected)
}

@Composable
fun YearAheadContent(
    state: YearAheadUiState,
    onBack: () -> Unit,
    onScopeSelected: (AccountScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        val forecast = state.forecast
        if (forecast == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val currency = CurrencyCode.of(forecast.currencyCode)
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
                item { Header(forecast, onBack, cards) }
                if (forecast.latest == null) {
                    item { EmptyState(stringResource(Res.string.future_empty_title), stringResource(Res.string.future_empty_body), cards) }
                    return@LazyColumn
                }
                if (forecast.scopes.size > 1) {
                    item {
                        ChipRow {
                            forecast.scopes.forEach { scope ->
                                ChoiceChip(scopeLabel(scope, state.accountNames), selected = scope == forecast.scope, onClick = {
                                    onScopeSelected(scope)
                                })
                            }
                        }
                    }
                }
                item { Totals(forecast, currency, cards) }
                item { MonthByMonth(forecast, currency, cards) }
                if (forecast.peaks.isNotEmpty()) item { Peaks(forecast, currency, cards) }
                if (forecast.years.isNotEmpty()) item { YearByYear(forecast, currency, cards) }
            }
        }
    }
}

@Composable
private fun Header(forecast: Forecast, onBack: () -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedIconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(NetWorthIcons.ChevronLeft, contentDescription = stringResource(Res.string.forecast_back), modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(Res.string.forecast_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            forecast.months.takeIf { it.isNotEmpty() }?.let { months ->
                val first = months.first().month
                val last = months.last().month
                Text(
                    stringResource(
                        Res.string.forecast_subtitle,
                        shortMonthYear(first.month, first.year),
                        shortMonthYear(last.month, last.year),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun scopeLabel(scope: AccountScope, names: Map<String, String>): String = when (scope) {
    AccountScope.All -> stringResource(Res.string.forecast_all_accounts)
    AccountScope.Children -> stringResource(Res.string.forecast_kids_savings)
    is AccountScope.Single -> names[scope.accountId].orEmpty()
}

@Composable
private fun Totals(forecast: Forecast, currency: CurrencyCode, modifier: Modifier) {
    val finance = NetWorthTheme.financeColors
    val fraction = currency.fractionDigits
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HeroCard(Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(Res.string.forecast_predicted_spending),
                    style = MaterialTheme.typography.labelSmall,
                    color = finance.heroMuted,
                )
                Text(
                    MoneyFormatter.whole(FutureFormat.roundTo(forecast.yearSpendMinor, 10, fraction), currency),
                    style = MaterialTheme.typography.headlineSmall,
                )
                forecast.changePercent?.let {
                    Text(
                        if (it >= 0) {
                            stringResource(Res.string.forecast_vs_last_up, it)
                        } else {
                            stringResource(Res.string.forecast_vs_last_down, -it)
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        SectionCard(Modifier.weight(1f), spacing = 4) {
            val save = forecast.yearSaveMinor
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            if (save != null) {
                Text(stringResource(Res.string.forecast_predicted_savings), style = MaterialTheme.typography.labelSmall, color = muted)
                Text(
                    MoneyFormatter.whole(FutureFormat.roundTo(save, 10, fraction), currency),
                    style = MaterialTheme.typography.headlineSmall,
                )
                forecast.lastYearSaveMinor?.let { last ->
                    val amount = MoneyFormatter.whole(FutureFormat.roundTo(last, 10, fraction), currency)
                    Text(
                        stringResource(if (save <= last) Res.string.forecast_down_from else Res.string.forecast_up_from, amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                    )
                }
            } else {
                Text(stringResource(Res.string.forecast_share), style = MaterialTheme.typography.labelSmall, color = muted)
                Text("${forecast.shareOfFamilyPercent ?: 0}%", style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.forecast_share_note), style = MaterialTheme.typography.labelSmall, color = muted)
            }
        }
    }
}

@Composable
private fun MonthByMonth(forecast: Forecast, currency: CurrencyCode, modifier: Modifier) {
    val over = NetWorthTheme.financeColors.negative
    val typical = NetWorthTheme.financeColors.chartMuted
    SectionCard(modifier, spacing = 12) {
        CardTitle(
            stringResource(Res.string.forecast_month_by_month),
            trailing = forecast.monthlyIncomeMinor?.let { stringResource(Res.string.forecast_income, MoneyFormatter.whole(it, currency)) },
        )
        ForecastBars(
            bars = forecast.months.map { ForecastBar(shortMonthName(it.month.month).take(1), it.spendMinor, it.overIncome) },
            income = forecast.monthlyIncomeMinor,
            overColor = over,
            overLabelColor = over,
            typicalColor = typical,
            description = stringResource(Res.string.forecast_chart_description),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (forecast.monthlyIncomeMinor != null) Legend(over, stringResource(Res.string.forecast_over_income))
            Legend(typical, stringResource(Res.string.forecast_typical))
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ColorKey(color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Peaks(forecast: Forecast, currency: CurrencyCode, modifier: Modifier) {
    val finance = NetWorthTheme.financeColors
    val and = stringResource(Res.string.future_and, "{a}", "{b}")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = finance.warningContainer,
        contentColor = finance.onWarningContainer,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(Res.string.forecast_peaks),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics {
                    heading()
                },
            )
            forecast.peaks.forEach { peak ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        shortMonthName(peak.month.month),
                        style = bold(MaterialTheme.typography.bodyMedium),
                        modifier = Modifier.width(36.dp),
                    )
                    Text(
                        peak.reason ?: stringResource(Res.string.forecast_peak_other),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        MoneyFormatter.whole(FutureFormat.roundTo(peak.spendMinor, 10, currency.fractionDigits), currency),
                        style = bold(MaterialTheme.typography.bodyMedium),
                    )
                }
            }
            forecast.setAsideMinor?.let { amount ->
                val months = forecast.coveredMonths.map { shortMonthName(it.month) }
                Text(
                    pluralStringResource(
                        Res.plurals.forecast_set_aside,
                        months.size,
                        MoneyFormatter.whole(amount, currency),
                        FutureFormat.joinWithAnd(months) { a, b -> and.replace("{a}", a).replace("{b}", b) },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun YearByYear(forecast: Forecast, currency: CurrencyCode, modifier: Modifier) {
    val finance = NetWorthTheme.financeColors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    SectionCard(modifier, spacing = 4) {
        CardTitle(stringResource(Res.string.forecast_year_by_year), Modifier.padding(bottom = 4.dp))
        YearRowLayout(
            stringResource(Res.string.forecast_year),
            stringResource(Res.string.forecast_spend),
            stringResource(Res.string.forecast_save),
            stringResource(Res.string.forecast_savings),
            MaterialTheme.typography.labelSmall,
            muted,
            muted,
        )
        forecast.years.forEach { row ->
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
            YearRowLayout(
                row.year.toString(),
                MoneyFormatter.compact(row.spendMinor, currency),
                MoneyFormatter.compact(row.saveMinor, currency),
                MoneyFormatter.compact(row.balanceMinor, currency),
                MaterialTheme.typography.bodyMedium,
                MaterialTheme.colorScheme.onSurface,
                if (row.saveMinor < 0) finance.negative else finance.positive,
                Modifier.padding(vertical = 10.dp),
            )
        }
        val perYear = MoneyFormatter.whole(FutureFormat.roundTo(forecast.trendPerYearMinor, 10, currency.fractionDigits), currency)
        Text(
            when (forecast.trend) {
                Trend.FLAT -> stringResource(Res.string.forecast_trend_flat)
                Trend.SHRINKING -> stringResource(Res.string.forecast_trend_shrinking, perYear)
                Trend.GROWING -> stringResource(Res.string.forecast_trend_growing, perYear)
            },
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun YearRowLayout(
    year: String,
    spend: String,
    save: String,
    balance: String,
    style: TextStyle,
    color: Color,
    saveColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(year, style = bold(style), color = color, modifier = Modifier.width(52.dp))
        Text(spend, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(
            save,
            style = style.copy(fontWeight = FontWeight.SemiBold),
            color = saveColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Text(balance, style = bold(style), color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

private fun bold(style: TextStyle) = style.copy(fontWeight = FontWeight.Bold)

@Preview
@Composable
private fun YearAheadPreview() {
    NetWorthTheme {
        YearAheadContent(previewYearAheadState(), {}, {})
    }
}

@Preview
@Composable
private fun YearAheadAccountDarkPreview() {
    NetWorthTheme(darkTheme = true) {
        YearAheadContent(previewYearAheadState(AccountScope.Single("a1")), {}, {})
    }
}
