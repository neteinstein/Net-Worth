package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme

/** The dark ink card that carries a screen's headline figure ("Saved in September €730"). */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val finance = NetWorthTheme.financeColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = finance.heroContainer,
        contentColor = finance.onHeroContainer,
    ) {
        CompositionLocalProvider(LocalContentColor provides finance.onHeroContainer) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
        }
    }
}
