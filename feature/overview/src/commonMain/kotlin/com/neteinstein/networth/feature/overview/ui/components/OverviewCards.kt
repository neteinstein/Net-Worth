package com.neteinstein.networth.feature.overview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.ui.components.BarSegment
import com.neteinstein.networth.core.designsystem.ui.components.CardTitle
import com.neteinstein.networth.core.designsystem.ui.components.ColorKey
import com.neteinstein.networth.core.designsystem.ui.components.ColumnBar
import com.neteinstein.networth.core.designsystem.ui.components.ColumnBarChart
import com.neteinstein.networth.core.designsystem.ui.components.HeroCard
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.components.StackedBar
import com.neteinstein.networth.core.designsystem.ui.components.categoryName
import com.neteinstein.networth.core.designsystem.ui.components.monthNames
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthNames
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.designsystem.ui.theme.categoryColor
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.feature.overview.domain.model.TrendStep
import com.neteinstein.networth.feature.overview.domain.usecase.AccountLine
import com.neteinstein.networth.feature.overview.domain.usecase.CategoryShare
import com.neteinstein.networth.feature.overview.domain.usecase.Overview
import com.neteinstein.networth.feature.overview.domain.usecase.SpikeSummary
import com.neteinstein.networth.feature.overview.generated.resources.Res
import com.neteinstein.networth.feature.overview.generated.resources.overview_account_count
import com.neteinstein.networth.feature.overview.generated.resources.overview_by_month
import com.neteinstein.networth.feature.overview.generated.resources.overview_by_quarter
import com.neteinstein.networth.feature.overview.generated.resources.overview_by_year
import com.neteinstein.networth.feature.overview.generated.resources.overview_empty_body
import com.neteinstein.networth.feature.overview.generated.resources.overview_empty_title
import com.neteinstein.networth.feature.overview.generated.resources.overview_import_extract
import com.neteinstein.networth.feature.overview.generated.resources.overview_income
import com.neteinstein.networth.feature.overview.generated.resources.overview_last_months
import com.neteinstein.networth.feature.overview.generated.resources.overview_mark_seasonal
import com.neteinstein.networth.feature.overview.generated.resources.overview_monthly_average
import com.neteinstein.networth.feature.overview.generated.resources.overview_more_categories
import com.neteinstein.networth.feature.overview.generated.resources.overview_net_worth
import com.neteinstein.networth.feature.overview.generated.resources.overview_no_accounts
import com.neteinstein.networth.feature.overview.generated.resources.overview_nothing_spent
import com.neteinstein.networth.feature.overview.generated.resources.overview_nothing_spent_period
import com.neteinstein.networth.feature.overview.generated.resources.overview_overspent_all_time
import com.neteinstein.networth.feature.overview.generated.resources.overview_overspent_in
import com.neteinstein.networth.feature.overview.generated.resources.overview_percent_of_income
import com.neteinstein.networth.feature.overview.generated.resources.overview_period_trend_description
import com.neteinstein.networth.feature.overview.generated.resources.overview_projection
import com.neteinstein.networth.feature.overview.generated.resources.overview_quarter
import com.neteinstein.networth.feature.overview.generated.resources.overview_review
import com.neteinstein.networth.feature.overview.generated.resources.overview_review_banner
import com.neteinstein.networth.feature.overview.generated.resources.overview_saved_all_time
import com.neteinstein.networth.feature.overview.generated.resources.overview_saved_in
import com.neteinstein.networth.feature.overview.generated.resources.overview_spent
import com.neteinstein.networth.feature.overview.generated.resources.overview_spike_body
import com.neteinstein.networth.feature.overview.generated.resources.overview_spike_title
import com.neteinstein.networth.feature.overview.generated.resources.overview_trend_description
import com.neteinstein.networth.feature.overview.generated.resources.overview_twelve_month_average
import com.neteinstein.networth.feature.overview.generated.resources.overview_type_current
import com.neteinstein.networth.feature.overview.generated.resources.overview_type_investment
import com.neteinstein.networth.feature.overview.generated.resources.overview_type_loan
import com.neteinstein.networth.feature.overview.generated.resources.overview_type_mortgage
import com.neteinstein.networth.feature.overview.generated.resources.overview_type_savings
import com.neteinstein.networth.feature.overview.generated.resources.overview_type_term_deposit
import com.neteinstein.networth.feature.overview.generated.resources.overview_uncategorised
import com.neteinstein.networth.feature.overview.generated.resources.overview_where_it_went
import com.neteinstein.networth.feature.overview.ui.OverviewLabels
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun SavedHeroCard(overview: Overview, currency: CurrencyCode, modifier: Modifier = Modifier) {
    val finance = NetWorthTheme.financeColors
    val period = OverviewLabels.inline(overview.period, shortMonthNames(), monthNames())
    val saved = overview.savedMinor >= 0
    HeroCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                when {
                    period == null -> stringResource(
                        if (saved) Res.string.overview_saved_all_time else Res.string.overview_overspent_all_time,
                    )
                    else -> stringResource(if (saved) Res.string.overview_saved_in else Res.string.overview_overspent_in, period)
                },
                style = MaterialTheme.typography.bodySmall,
                color = finance.heroMuted,
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(MoneyFormatter.whole(abs(overview.savedMinor), currency), style = MaterialTheme.typography.displayLarge)
                overview.savedPercent?.takeIf { it >= 0 }?.let {
                    Text(
                        stringResource(Res.string.overview_percent_of_income, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = finance.heroMuted,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
        }
        val spentWeight = overview.spentMinor.coerceAtLeast(0).toFloat()
        val savedWeight = overview.savedMinor.coerceAtLeast(0).toFloat()
        if (spentWeight + savedWeight > 0f) {
            StackedBar(
                listOf(BarSegment(spentWeight, finance.heroAccent), BarSegment(savedWeight, finance.onHeroContainer)),
                height = 10.dp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroFigure(
                stringResource(Res.string.overview_income),
                MoneyFormatter.whole(overview.incomeMinor, currency),
                Modifier.weight(1f),
            )
            HeroFigure(stringResource(Res.string.overview_spent), MoneyFormatter.whole(overview.spentMinor, currency), Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroFigure(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = NetWorthTheme.financeColors.heroMuted)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
internal fun SpikeCard(spike: SpikeSummary, currency: CurrencyCode, onMarkSeasonal: () -> Unit, modifier: Modifier = Modifier) {
    val finance = NetWorthTheme.financeColors
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = finance.warningContainer,
        contentColor = finance.onWarningContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = finance.negative, contentColor = Color.White, modifier = Modifier.size(32.dp)) {
                    Icon(NetWorthIcons.TrendUp, contentDescription = null, modifier = Modifier.padding(7.dp))
                }
                Text(
                    stringResource(Res.string.overview_spike_title, categoryName(spike.categoryName)),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                stringResource(
                    Res.string.overview_spike_body,
                    MoneyFormatter.whole(spike.spentMinor, currency),
                    ((spike.ratio * 10).roundToInt() / 10.0).toString(),
                    MoneyFormatter.whole(spike.averageMinor, currency),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onMarkSeasonal,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = finance.negative, contentColor = Color.White),
                modifier = Modifier.heightIn(min = 44.dp),
            ) { Text(stringResource(Res.string.overview_mark_seasonal)) }
        }
    }
}

@Composable
internal fun WhereItWentCard(overview: Overview, currency: CurrencyCode, modifier: Modifier = Modifier) {
    SectionCard(modifier) {
        CardTitle(stringResource(Res.string.overview_where_it_went))
        if (overview.shares.isEmpty()) {
            Text(
                stringResource(
                    if (overview.period.isMonth) Res.string.overview_nothing_spent else Res.string.overview_nothing_spent_period,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        StackedBar(overview.shares.map { BarSegment(it.amountMinor.toFloat(), it.color()) })
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            overview.topShares.forEach { share ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ColorKey(share.color())
                    Text(
                        share.name?.let { categoryName(it) } ?: stringResource(Res.string.overview_uncategorised),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${share.percent}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(40.dp),
                    )
                    Text(
                        MoneyFormatter.whole(share.amountMinor, currency),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(72.dp),
                    )
                }
            }
            val others = overview.otherShares
            if (others.isNotEmpty()) {
                Text(
                    pluralStringResource(
                        Res.plurals.overview_more_categories,
                        others.size,
                        others.size,
                        MoneyFormatter.whole(
                            others.sumOf {
                                it.amountMinor
                            },
                            currency,
                        ),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryShare.color(): Color = colorHex?.let { categoryColor(it) } ?: MaterialTheme.colorScheme.outline

@Composable
internal fun TrendCard(overview: Overview, currency: CurrencyCode, modifier: Modifier = Modifier) {
    val isMonth = overview.period.isMonth
    val step = overview.trend.firstOrNull()?.step ?: TrendStep.MONTH
    val average = overview.averageSpentMinor.takeIf { it > 0 }
    SectionCard(modifier) {
        CardTitle(
            stringResource(
                when {
                    isMonth -> Res.string.overview_last_months
                    step == TrendStep.MONTH -> Res.string.overview_by_month
                    step == TrendStep.QUARTER -> Res.string.overview_by_quarter
                    else -> Res.string.overview_by_year
                },
            ),
            trailing = average?.let {
                stringResource(
                    if (isMonth) Res.string.overview_twelve_month_average else Res.string.overview_monthly_average,
                    MoneyFormatter.whole(it, currency),
                )
            },
        )
        val short = shortMonthNames()
        val quarter = stringResource(Res.string.overview_quarter)
        ColumnBarChart(
            bars = overview.trend.map { point ->
                ColumnBar(
                    label = OverviewLabels.bar(point, short, quarter),
                    valueLabel = MoneyFormatter.compact(point.spentMinor, currency),
                    value = point.spentMinor.toFloat(),
                    highlighted = point.isSelected,
                )
            },
            // The dashed line is a monthly figure, so it only belongs on monthly bars.
            average = overview.averageSpentMinor.toFloat().takeIf { step == TrendStep.MONTH },
            description = stringResource(
                if (isMonth) Res.string.overview_trend_description else Res.string.overview_period_trend_description,
                MoneyFormatter.whole(overview.spentMinor, currency),
            ),
        )
    }
}

@Composable
internal fun NetWorthCard(overview: Overview, currency: CurrencyCode, onOpenFuture: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier, spacing = 10) {
        CardTitle(stringResource(Res.string.overview_net_worth))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(MoneyFormatter.whole(overview.netWorthMinor, currency), style = MaterialTheme.typography.displaySmall)
            Text(
                pluralStringResource(Res.plurals.overview_account_count, overview.accounts.size, overview.accounts.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (overview.accounts.isEmpty()) {
            Text(
                stringResource(Res.string.overview_no_accounts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        overview.accounts.forEach { AccountRow(it, currency) }
        overview.projectedSavingsMinor?.let {
            Surface(
                onClick = onOpenFuture,
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.overview_projection, MoneyFormatter.whole(it, currency), Overview.PROJECTION_YEARS),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        NetWorthIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(account: AccountLine, currency: CurrencyCode) {
    val finance = NetWorthTheme.financeColors
    val dot = when {
        account.type.isLiability -> MaterialTheme.colorScheme.onSurfaceVariant
        account.isChild -> finance.negative
        account.type == AccountType.CURRENT -> MaterialTheme.colorScheme.onSurface
        else -> finance.positive
    }
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
        Row(
            Modifier.heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ColorKey(dot, size = 8.dp)
            Text(account.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                stringResource(account.type.label()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val balance = if (account.type.isLiability) -account.balanceMinor else account.balanceMinor
            Text(
                MoneyFormatter.whole(balance, currency),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.End,
                modifier = Modifier.width(96.dp),
            )
        }
    }
}

private fun AccountType.label() = when (this) {
    AccountType.CURRENT -> Res.string.overview_type_current
    AccountType.SAVINGS -> Res.string.overview_type_savings
    AccountType.TERM_DEPOSIT -> Res.string.overview_type_term_deposit
    AccountType.INVESTMENT -> Res.string.overview_type_investment
    AccountType.MORTGAGE -> Res.string.overview_type_mortgage
    AccountType.LOAN -> Res.string.overview_type_loan
}

@Composable
internal fun EmptyHistoryCard(onImport: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier, spacing = 6) {
        Text(stringResource(Res.string.overview_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.overview_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onImport, modifier = Modifier.padding(top = 8.dp).heightIn(min = 44.dp)) {
            Text(stringResource(Res.string.overview_import_extract))
        }
    }
}

/** "18 transactions need a category · Review" — the way into Review & learn after an import. */
@Composable
internal fun ReviewBanner(count: Int, onReview: () -> Unit, modifier: Modifier = Modifier) {
    val finance = NetWorthTheme.financeColors
    Surface(
        onClick = onReview,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = finance.warningContainer,
        contentColor = finance.onWarningContainer,
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                pluralStringResource(Res.plurals.overview_review_banner, count, count),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(Res.string.overview_review), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Icon(NetWorthIcons.ChevronRight, contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
    }
}
