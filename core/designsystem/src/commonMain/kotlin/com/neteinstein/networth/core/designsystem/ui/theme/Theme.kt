package com.neteinstein.networth.core.designsystem.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

/**
 * App theme. [dynamicColor] uses the wallpaper-derived palette where the platform supports it
 * (Android 12+); elsewhere it falls back to the brand palette.
 */
@Composable
fun NetWorthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val target = (if (dynamicColor) platformDynamicColorScheme(darkTheme) else null)
        ?: if (darkTheme) DarkColors else LightColors

    SystemBarsAppearance(darkTheme = darkTheme)

    CompositionLocalProvider(
        LocalFinanceColors provides if (darkTheme) DarkFinanceColors else LightFinanceColors,
    ) {
        MaterialTheme(
            colorScheme = target.animated(),
            typography = NetWorthTypography,
            shapes = NetWorthShapes,
            content = content,
        )
    }
}

/** Access to the extra semantic colours, e.g. `NetWorthTheme.financeColors.positive`. */
object NetWorthTheme {
    val financeColors: FinanceColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFinanceColors.current
}

/** Cross-fades the key colours so switching light/dark feels smooth rather than abrupt. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 450)
    val primary by animateColorAsState(primary, spec)
    val primaryContainer by animateColorAsState(primaryContainer, spec)
    val secondary by animateColorAsState(secondary, spec)
    val background by animateColorAsState(background, spec)
    val surface by animateColorAsState(surface, spec)
    val surfaceContainer by animateColorAsState(surfaceContainer, spec)
    val surfaceContainerHigh by animateColorAsState(surfaceContainerHigh, spec)
    val onBackground by animateColorAsState(onBackground, spec)
    val onSurface by animateColorAsState(onSurface, spec)
    return copy(
        primary = primary,
        primaryContainer = primaryContainer,
        secondary = secondary,
        background = background,
        surface = surface,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        onBackground = onBackground,
        onSurface = onSurface,
    )
}
