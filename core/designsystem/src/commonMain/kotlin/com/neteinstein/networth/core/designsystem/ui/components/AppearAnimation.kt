package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Fades and slides its content in on first composition. Pass increasing [index] values to get a
 * staggered "cascade" across a list.
 */
@Composable
fun AppearAnimation(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val delay = (index * STAGGER_MILLIS).coerceAtMost(MAX_DELAY_MILLIS)
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 350, delayMillis = delay)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                initialOffsetY = { it / 3 },
            ),
    ) {
        content()
    }
}

private const val STAGGER_MILLIS = 60
private const val MAX_DELAY_MILLIS = 480
