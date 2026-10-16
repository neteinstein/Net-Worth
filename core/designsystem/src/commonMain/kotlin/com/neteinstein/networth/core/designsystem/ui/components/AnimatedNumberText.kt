package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Text that counts smoothly from its previous value to [value] (in minor units, e.g. cents).
 * Formatting stays with the caller so this component knows nothing about currencies.
 */
@Composable
fun AnimatedNumberText(
    value: Long,
    format: (Long) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    durationMillis: Int = 900,
) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animatable.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        )
    }
    val shown = if (animatable.isRunning) animatable.value.toLong() else value
    Text(text = format(shown), modifier = modifier, style = style, color = color)
}
