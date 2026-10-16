package com.neteinstein.networth.core.designsystem.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Whether the platform offers a wallpaper-derived ("Material You") palette. */
expect val supportsDynamicColor: Boolean

/** The platform's dynamic colour scheme, or `null` when unsupported. */
@Composable
internal expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

/**
 * Keeps system bar icons legible when the in-app theme differs from the system theme
 * (edge-to-edge means the app draws behind the bars).
 */
@Composable
internal expect fun SystemBarsAppearance(darkTheme: Boolean)
