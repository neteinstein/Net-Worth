package com.neteinstein.networth.core.designsystem.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual val supportsDynamicColor: Boolean = false

@Composable
internal actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

// Browsers have no system bars to tint.
@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean) = Unit
