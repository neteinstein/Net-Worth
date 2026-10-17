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
import com.neteinstein.networth.core.designsystem.ui.components.monthName
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthName
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.designsystem.ui.theme.colorFromHex
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.feature.overview.domain.usecase.AccountLine
import com.neteinstein.networth.feature.overview.domain.usecase.CategoryShare
import com.neteinstein.networth.feature.overview.domain.usecase.Overview
import com.neteinstein.networth.feature.overview.domain.usecase.SpikeSummary
import com.neteinstein.networth.feature.overview.generated.resources.Res
import com.neteinstein.networth.feature.overview.generated.resources.overview_account_count
import com.neteinstein.networth.feature.overview.generated.resources.overview_empty_body
import com.neteinstein.networth.feature.overview.generated.resources.overview_empty_title
import com.neteinstein.networth.feature.overview.generated.resources.overview_income
import com.neteinstein.networth.feature.overview.generated.resources.overview_last_months
import com.neteinstein.networth.feature.overview.generated.resources.overview_mark_seasonal
import com.neteinstein.networth.feature.overview.generated.resources.overview_more_categories
import com.neteinstein.networth.feature.overview.generated.resources.overview_net_worth
import com.neteinstein.networth.feature.overview.generated.resources.overview_no_accounts
import com.neteinstein.networth.feature.overview.generated.resources.overview_nothing_spent
import com.neteinstein.networth.feature.overview.generated.resources.overview_overspent_in
import com.neteinstein.networth.feature.overview.generated.resources.overview_percent_of_income
import com.neteinstein.networth.feature.overview.generated.resources.overview_projection
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
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun SavedHeroCard(overview: Overview, currency: CurrencyCode, modifier: Modifier = Modifier) {
    val finance = NetWorthTheme.financeColors
    val month = monthName(overview.month.month)
    HeroCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(if (overview.savedMinor >= 0) Res.string.overview_saved_in else Res.string.overview_overspent_in, month),
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
                Text(stringResource(Res.string.overview_spike_title, spike.categoryName), style = MaterialTheme.typography.titleSmall)
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
                stringResource(Res.string.overview_nothing_spent),
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
                        share.name ?: stringResource(Res.string.overview_uncategorised),
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
private fun CategoryShare.color(): Color = colorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.outline

@Composable
internal fun TrendCard(overview: Overview, currency: CurrencyCode, modifier: Modifier = Modifier) {
    SectionCard(modifier) {
        CardTitle(
            stringResource(Res.string.overview_last_months),
            trailing = overview.averageSpentMinor.takeIf { it > 0 }?.let {
                stringResource(Res.string.overview_twelve_month_average, MoneyFormatter.whole(it, currency))
            },
        )
        ColumnBarChart(
            bars = overview.trend.map { point ->
                ColumnBar(
                    label = shortMonthName(point.month.month),
                    valueLabel = MoneyFormatter.compact(point.spentMinor, currency),
                    value = point.spentMinor.toFloat(),
                    highlighted = point.isSelected,
                )
            },
            average = overview.averageSpentMinor.toFloat(),
            description = stringResource(Res.string.overview_trend_description, MoneyFormatter.whole(overview.spentMinor, currency)),
        )
    }
}

@Composable
internal fun NetWorthCard(overview: Overview, currency: CurrencyCode, modifier: Modifier = Modifier) {
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
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(
                    stringResource(Res.string.overview_projection, MoneyFormatter.whole(it, currency), Overview.PROJECTION_YEARS),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                )
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
internal fun EmptyHistoryCard(modifier: Modifier = Modifier) {
    SectionCard(modifier, spacing = 6) {
        Text(stringResource(Res.string.overview_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.overview_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
