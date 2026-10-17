package com.neteinstein.networth.feature.alerts.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.CardTitle
import com.neteinstein.networth.core.designsystem.ui.components.ColorKey
import com.neteinstein.networth.core.designsystem.ui.components.EmptyState
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.components.OverlineLabel
import com.neteinstein.networth.core.designsystem.ui.components.ProportionBar
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.components.monthNames
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthNames
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.designsystem.ui.theme.colorFromHex
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.feature.alerts.domain.usecase.AccountName
import com.neteinstein.networth.feature.alerts.domain.usecase.AccountShare
import com.neteinstein.networth.feature.alerts.domain.usecase.Alerts
import com.neteinstein.networth.feature.alerts.domain.usecase.GoodNews
import com.neteinstein.networth.feature.alerts.domain.usecase.Insight
import com.neteinstein.networth.feature.alerts.domain.usecase.SpikeAlert
import com.neteinstein.networth.feature.alerts.generated.resources.Res
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_account_personal
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_both_current
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_empty_body
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_empty_title
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_going_well
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_large_body
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_large_payment
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_mark_seasonal
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_mostly
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_mostly_personal
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_new_recurring
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_no_spikes
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_other
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_purchases_between
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_recurring_body
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_repeats
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_rising_body
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_rising_title
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_seasonal_planned
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_see_transactions
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_spike_title
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_spike_vs_avg
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_spikes_in
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_spread
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_streak
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_streak_each
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_streak_single
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_threshold
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_title
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_under_average
import com.neteinstein.networth.feature.alerts.generated.resources.alerts_worth_a_look
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlertsScreen(
    onOpenAlertRules: () -> Unit,
    onSeeTransactions: (categoryId: String, month: CalendarMonth) -> Unit,
    viewModel: AlertsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlertsContent(state, onOpenAlertRules, viewModel::onToggleSeasonal, onSeeTransactions)
}

@Composable
fun AlertsContent(
    state: AlertsUiState,
    onOpenAlertRules: () -> Unit,
    onToggleSeasonal: (SpikeAlert) -> Unit,
    onSeeTransactions: (String, CalendarMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        val alerts = state.alerts
        if (alerts == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val currency = CurrencyCode.of(alerts.currencyCode)
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
                item {
                    Row(cards.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.alerts_title),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                        OutlinedButton(onClick = onOpenAlertRules, shape = CircleShape, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(
                                stringResource(Res.string.alerts_threshold, alerts.thresholdPct),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
                val month = alerts.month
                if (month == null) {
                    item { EmptyState(stringResource(Res.string.alerts_empty_title), stringResource(Res.string.alerts_empty_body), cards) }
                    return@LazyColumn
                }
                item { OverlineLabel(stringResource(Res.string.alerts_spikes_in, monthNames()[month.month - 1]), cards) }
                if (alerts.spikes.isEmpty()) {
                    item {
                        val monthName = monthNames()[month.month - 1]
                        Text(
                            stringResource(Res.string.alerts_no_spikes, monthName, alerts.thresholdPct),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = cards,
                        )
                    }
                }
                items(alerts.spikes, key = { it.categoryId }) { spike ->
                    SpikeCard(spike, expanded = spike == alerts.spikes.first(), currency, onToggleSeasonal, onSeeTransactions, cards)
                }
                if (alerts.worthALook.isNotEmpty()) {
                    item { OverlineLabel(stringResource(Res.string.alerts_worth_a_look), cards) }
                    items(alerts.worthALook) { insight -> InsightCard(insight, currency, cards) }
                }
                if (alerts.goingWell.isNotEmpty()) {
                    item { GoingWellCard(alerts.goingWell, currency, cards) }
                }
            }
        }
    }
}

@Composable
private fun SpikeCard(
    spike: SpikeAlert,
    expanded: Boolean,
    currency: CurrencyCode,
    onToggleSeasonal: (SpikeAlert) -> Unit,
    onSeeTransactions: (String, CalendarMonth) -> Unit,
    modifier: Modifier,
) {
    val finance = NetWorthTheme.financeColors
    val months = shortMonthNames()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (spike.seasonal) MaterialTheme.colorScheme.surface else finance.warningContainer,
        contentColor = if (spike.seasonal) MaterialTheme.colorScheme.onSurface else finance.onWarningContainer,
        border = BorderStroke(1.dp, if (spike.seasonal) MaterialTheme.colorScheme.outline else finance.warningOutline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(Res.string.alerts_spike_title, spike.name, MoneyFormatter.whole(spike.spentMinor, currency)),
                    style = MaterialTheme.typography.titleMedium,
                )
                val share = spike.accounts?.let { accountShare(it) }
                Text(
                    listOfNotNull(
                        stringResource(
                            Res.string.alerts_spike_vs_avg,
                            spike.percentAboveAverage,
                            MoneyFormatter.whole(spike.averageMinor, currency),
                        ),
                        share,
                    )
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (expanded && spike.parts.isNotEmpty()) {
                val max = spike.parts.maxOf { it.second }.coerceAtLeast(1)
                spike.parts.forEach { (name, amount) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name ?: stringResource(Res.string.alerts_other),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.45f),
                        )
                        ProportionBar(amount.toFloat() / max, colorFromHex(spike.colorHex), Modifier.weight(0.4f))
                        Spacer(Modifier.width(8.dp))
                        Text(MoneyFormatter.whole(amount, currency), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            val lastYear = spike.sameMonthLastYearMinor
            if (expanded && spike.repeatsEveryYear && lastYear != null) {
                Text(
                    stringResource(
                        Res.string.alerts_repeats,
                        monthNames()[spike.month.month - 1],
                        MoneyFormatter.whole(lastYear, currency),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val first = spike.firstDay
            val last = spike.lastDay
            if (!expanded && first != null && last != null) {
                Text(
                    pluralStringResource(
                        Res.plurals.alerts_purchases_between,
                        spike.purchases,
                        spike.purchases,
                        dayLabel(first, last, months),
                        shortDay(last, months),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (expanded || spike.seasonal) {
                    val next = spike.month.plus(12)
                    Button(
                        onClick = { onToggleSeasonal(spike) },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = if (spike.seasonal) finance.heroContainer else finance.negative,
                                contentColor = finance.onHeroContainer,
                            ),
                        modifier = Modifier.heightIn(min = 44.dp),
                    ) {
                        Text(
                            if (spike.seasonal) {
                                stringResource(Res.string.alerts_seasonal_planned, "${months[next.month - 1]} ${next.year}")
                            } else {
                                stringResource(Res.string.alerts_mark_seasonal)
                            },
                        )
                    }
                }
                TextButton(onClick = { onSeeTransactions(spike.categoryId, spike.month) }) {
                    Text(stringResource(Res.string.alerts_see_transactions), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun accountShare(share: AccountShare): String =
    when (share) {
        is AccountShare.Mostly ->
            if (share.account.isPersonal) {
                stringResource(Res.string.alerts_mostly_personal, share.account.name)
            } else {
                stringResource(Res.string.alerts_mostly, share.account.name)
            }
        AccountShare.BothCurrent -> stringResource(Res.string.alerts_both_current)
        is AccountShare.Spread -> pluralStringResource(Res.plurals.alerts_spread, share.accounts, share.accounts)
    }

@Composable
private fun accountLabel(account: AccountName): String =
    if (account.isPersonal) stringResource(Res.string.alerts_account_personal, account.name) else account.name

private fun shortDay(
    date: LocalDate,
    months: List<String>,
): String = "${date.day} ${months[date.month.number - 1]}"

/** "2" when both days share a month (as in "between 2 and 11 Sep"), else "28 Aug". */
private fun dayLabel(
    first: LocalDate,
    last: LocalDate,
    months: List<String>,
): String = if (first.month == last.month && first.year == last.year) first.day.toString() else shortDay(first, months)

@Composable
private fun InsightCard(
    insight: Insight,
    currency: CurrencyCode,
    modifier: Modifier,
) {
    val months = shortMonthNames()
    SectionCard(modifier, spacing = 4) {
        when (insight) {
            is Insight.NewRecurring -> {
                Text(stringResource(Res.string.alerts_new_recurring), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(
                        Res.string.alerts_recurring_body,
                        insight.name,
                        MoneyFormatter.format(insight.amountMinor, currency),
                        shortDay(insight.since, months),
                        accountLabel(insight.account),
                        MoneyFormatter.whole(insight.yearlyMinor, currency),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is Insight.Rising -> {
                Text(stringResource(Res.string.alerts_rising_title, insight.name), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(
                        Res.string.alerts_rising_body,
                        insight.values.joinToString(" → ") { MoneyFormatter.whole(it, currency) },
                        months[insight.first.month - 1],
                        months[insight.last.month - 1],
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is Insight.LargePayment -> {
                Text(stringResource(Res.string.alerts_large_payment), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(
                        Res.string.alerts_large_body,
                        insight.description,
                        MoneyFormatter.format(insight.amountMinor, currency),
                        shortDay(insight.date, months),
                        insight.accountName,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GoingWellCard(
    news: List<GoodNews>,
    currency: CurrencyCode,
    modifier: Modifier,
) {
    val finance = NetWorthTheme.financeColors
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = finance.goodContainer,
        contentColor = finance.onGoodContainer,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardTitle(stringResource(Res.string.alerts_going_well))
            news.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorKey(finance.positive)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when (item) {
                            is GoodNews.UnderAverage ->
                                stringResource(
                                    Res.string.alerts_under_average,
                                    item.name,
                                    item.percentBelow,
                                    MoneyFormatter.whole(item.underMinor, currency),
                                )
                            is GoodNews.SavingsStreak -> {
                                val each = item.amountEachMinor
                                when {
                                    each == null -> stringResource(Res.string.alerts_streak, item.months)
                                    item.accounts > 1 -> stringResource(
                                        Res.string.alerts_streak_each,
                                        MoneyFormatter.whole(each, currency),
                                        item.months,
                                    )
                                    else -> stringResource(
                                        Res.string.alerts_streak_single,
                                        MoneyFormatter.whole(each, currency),
                                        item.months,
                                    )
                                }
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

internal fun previewAlerts(): Alerts {
    val sep = CalendarMonth(2026, 9)
    return Alerts(
        currencyCode = "EUR",
        month = sep,
        thresholdPct = 40,
        spikes =
            listOf(
                SpikeAlert(
                    "s",
                    "School",
                    "#D9652B",
                    sep,
                    610_00,
                    285_00,
                    114,
                    AccountShare.BothCurrent,
                    listOf("Books & materials" to 385_00L, "Canteen top-ups" to 150_00L, "Parents’ association" to 45_00L, null to 30_00L),
                    580_00,
                    true,
                    6,
                    LocalDate(2026, 9, 1),
                    LocalDate(2026, 9, 12),
                    false,
                ),
                SpikeAlert(
                    "c",
                    "Clothes",
                    "#A8487A",
                    sep,
                    285_00,
                    195_00,
                    46,
                    AccountShare.Mostly(AccountName("Maria", true)),
                    emptyList(),
                    null,
                    false,
                    5,
                    LocalDate(2026, 9, 2),
                    LocalDate(2026, 9, 11),
                    false,
                ),
            ),
        worthALook =
            listOf(
                Insight.NewRecurring("Disney+", 13_99, LocalDate(2026, 9, 8), AccountName("Pedro", true)),
                Insight.Rising("Fuel", listOf(212_00, 226_00, 238_00), CalendarMonth(2026, 7), sep),
            ),
        goingWell = listOf(GoodNews.UnderAverage("Dining & leisure", 21, 60_00), GoodNews.SavingsStreak(3, 30_00, 9)),
    )
}

@Preview
@Composable
private fun AlertsPreview() {
    NetWorthTheme { AlertsContent(AlertsUiState(previewAlerts()), {}, {}, { _, _ -> }) }
}

@Preview
@Composable
private fun AlertsEmptyDarkPreview() {
    NetWorthTheme(darkTheme = true) {
        AlertsContent(AlertsUiState(Alerts("EUR", null, 40, emptyList(), emptyList(), emptyList())), {}, {}, { _, _ -> })
    }
}
