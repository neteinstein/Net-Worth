package com.neteinstein.networth.core.designsystem.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette: deep emerald ("growth") with a warm gold accent ("value").
internal object BrandColors {
    val Emerald10 = Color(0xFF00210F)
    val Emerald20 = Color(0xFF00391D)
    val Emerald30 = Color(0xFF00522C)
    val Emerald40 = Color(0xFF006D3B)
    val Emerald80 = Color(0xFF6CDB9A)
    val Emerald90 = Color(0xFF89F8B4)
    val Gold30 = Color(0xFF574500)
    val Gold40 = Color(0xFF735C00)
    val Gold80 = Color(0xFFE9C349)
    val Gold90 = Color(0xFFFFE08B)
    val Teal40 = Color(0xFF00677F)
    val Teal80 = Color(0xFF5FD4F7)
    val Red40 = Color(0xFFBA1A1A)
    val Red80 = Color(0xFFFFB4AB)
    val Neutral6 = Color(0xFF0F1511)
    val Neutral10 = Color(0xFF171D19)
    val Neutral12 = Color(0xFF1B211D)
    val Neutral17 = Color(0xFF252B27)
    val Neutral22 = Color(0xFF303632)
    val Neutral90 = Color(0xFFDEE4DE)
    val Neutral94 = Color(0xFFEAEFE9)
    val Neutral96 = Color(0xFFF0F5EF)
    val Neutral98 = Color(0xFFF5FBF4)
    val NeutralVariant30 = Color(0xFF404943)
    val NeutralVariant50 = Color(0xFF707972)
    val NeutralVariant80 = Color(0xFFC0C9C1)
    val NeutralVariant90 = Color(0xFFDCE5DC)
}

internal val LightColors = lightColorScheme(
    primary = BrandColors.Emerald40,
    onPrimary = Color.White,
    primaryContainer = BrandColors.Emerald90,
    onPrimaryContainer = BrandColors.Emerald10,
    secondary = BrandColors.Gold40,
    onSecondary = Color.White,
    secondaryContainer = BrandColors.Gold90,
    onSecondaryContainer = Color(0xFF231B00),
    tertiary = BrandColors.Teal40,
    onTertiary = Color.White,
    error = BrandColors.Red40,
    background = BrandColors.Neutral98,
    onBackground = BrandColors.Neutral10,
    surface = BrandColors.Neutral98,
    onSurface = BrandColors.Neutral10,
    surfaceVariant = BrandColors.NeutralVariant90,
    onSurfaceVariant = BrandColors.NeutralVariant30,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = BrandColors.Neutral96,
    surfaceContainer = BrandColors.Neutral94,
    surfaceContainerHigh = BrandColors.Neutral90,
    outline = BrandColors.NeutralVariant50,
    outlineVariant = BrandColors.NeutralVariant80,
)

internal val DarkColors = darkColorScheme(
    primary = BrandColors.Emerald80,
    onPrimary = BrandColors.Emerald20,
    primaryContainer = BrandColors.Emerald30,
    onPrimaryContainer = BrandColors.Emerald90,
    secondary = BrandColors.Gold80,
    onSecondary = Color(0xFF3C2F00),
    secondaryContainer = BrandColors.Gold30,
    onSecondaryContainer = BrandColors.Gold90,
    tertiary = BrandColors.Teal80,
    onTertiary = Color(0xFF003544),
    error = BrandColors.Red80,
    background = BrandColors.Neutral6,
    onBackground = BrandColors.Neutral90,
    surface = BrandColors.Neutral6,
    onSurface = BrandColors.Neutral90,
    surfaceVariant = BrandColors.NeutralVariant30,
    onSurfaceVariant = BrandColors.NeutralVariant80,
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = BrandColors.Neutral10,
    surfaceContainer = BrandColors.Neutral12,
    surfaceContainerHigh = BrandColors.Neutral17,
    surfaceContainerHighest = BrandColors.Neutral22,
    outline = Color(0xFF8A938B),
    outlineVariant = BrandColors.NeutralVariant30,
)

/** Semantic colours Material 3 has no slot for (gains vs. losses). */
data class FinanceColors(
    val positive: Color,
    val negative: Color,
)

internal val LightFinanceColors = FinanceColors(positive = Color(0xFF1B8A4B), negative = Color(0xFFC62828))
internal val DarkFinanceColors = FinanceColors(positive = Color(0xFF7BE0A4), negative = Color(0xFFFF8A80))
