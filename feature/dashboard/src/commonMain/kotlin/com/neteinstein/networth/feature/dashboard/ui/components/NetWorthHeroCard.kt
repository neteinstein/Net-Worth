package com.neteinstein.networth.feature.dashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.ui.components.AnimatedNumberText
import com.neteinstein.networth.core.designsystem.ui.components.HeroCard
import com.neteinstein.networth.feature.dashboard.domain.model.NetWorthSummary
import com.neteinstein.networth.feature.dashboard.generated.resources.Res
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_assets
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_debt_ratio
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_liabilities
import com.neteinstein.networth.feature.dashboard.generated.resources.dashboard_net_worth_label
import com.neteinstein.networth.feature.dashboard.ui.MoneyFormatter
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun NetWorthHeroCard(
    summary: NetWorthSummary,
    modifier: Modifier = Modifier,
) {
    val onHero = MaterialTheme.colorScheme.onPrimary
    val netWorth = summary.netWorth
    HeroCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.dashboard_net_worth_label),
                style = MaterialTheme.typography.titleMedium,
                color = onHero.copy(alpha = 0.85f),
            )
            AnimatedNumberText(
                value = netWorth.minorUnits,
                format = { MoneyFormatter.format(it, netWorth.currency) },
                style = MaterialTheme.typography.displayMedium,
                color = onHero,
                modifier = Modifier.semantics { contentDescription = MoneyFormatter.format(netWorth) },
            )
            BreakdownBar(
                debtRatio = summary.debtRatio.toFloat(),
                assetColor = onHero,
                liabilityColor = MaterialTheme.colorScheme.secondary,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Total(stringResource(Res.string.dashboard_assets), MoneyFormatter.format(summary.totalAssets, showFraction = false), onHero)
                Total(
                    stringResource(Res.string.dashboard_liabilities),
                    MoneyFormatter.format(summary.totalLiabilities, showFraction = false),
                    onHero,
                )
            }
            Text(
                stringResource(Res.string.dashboard_debt_ratio, "${(summary.debtRatio * 100).roundToInt()}%"),
                style = MaterialTheme.typography.bodySmall,
                color = onHero.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun Total(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

/** Animated two-tone bar showing how much of the asset base is offset by liabilities. */
@Composable
private fun BreakdownBar(
    debtRatio: Float,
    assetColor: Color,
    liabilityColor: Color,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val fraction by animateFloatAsState(
        targetValue = if (started) debtRatio.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 1_100),
        label = "debt-ratio",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(assetColor.copy(alpha = 0.35f)),
    ) {
        Spacer(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(liabilityColor),
        )
    }
}
