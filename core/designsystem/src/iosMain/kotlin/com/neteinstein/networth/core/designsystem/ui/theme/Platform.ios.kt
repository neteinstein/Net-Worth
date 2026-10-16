package com.neteinstein.networth.core.designsystem.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual val supportsDynamicColor: Boolean = false

@Composable
internal actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

// The status bar style follows the hosting UIViewController / SwiftUI colour scheme.
@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean) = Unit
