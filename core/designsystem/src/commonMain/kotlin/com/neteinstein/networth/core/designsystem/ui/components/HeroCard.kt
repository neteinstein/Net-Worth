package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Large gradient card used for the headline figure of a screen. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = shape, ambientColor = colors.primary, spotColor = colors.primary)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(colors.primary, colors.tertiary.copy(alpha = 0.85f), colors.primaryContainer),
                ),
            )
            .padding(24.dp),
        content = content,
    )
}
