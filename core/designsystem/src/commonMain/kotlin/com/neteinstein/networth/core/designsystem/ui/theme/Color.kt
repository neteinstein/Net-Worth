package com.neteinstein.networth.core.designsystem.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Palette from the "Net Worth — family finances" design canvas (docs/design/): ink, a cool grey
// ground, white cards, one blue accent, warm orange for warnings and teal for good news. The dark
// palette keeps the same roles with lightness flipped.
internal object BrandColors {
    val Ink = Color(0xFF141B2D)
    val Ground = Color(0xFFF3F4F6)
    val Card = Color(0xFFFFFFFF)
    val Hairline = Color(0xFFEEF0F4)
    val Border = Color(0xFFE3E6EC)
    val Control = Color(0xFFD5D9E1)
    val Muted = Color(0xFF5A6275)
    val Accent = Color(0xFF2D55D6)
    val AccentDark = Color(0xFF1E3FA8)
    val AccentWash = Color(0xFFE6ECFB)
    val AccentSoft = Color(0xFF7D9BF2)
    val Orange = Color(0xFFB5481A)

    val NightGround = Color(0xFF0C111C)
    val NightCard = Color(0xFF161D2C)
    val NightContainer = Color(0xFF1F2738)
    val NightHigh = Color(0xFF2A3350)
    val NightBorder = Color(0xFF2A3346)
    val NightControl = Color(0xFF3A4459)
    val NightText = Color(0xFFE8EBF2)
    val NightMuted = Color(0xFFA7B0C2)
}

internal val LightColors = lightColorScheme(
    primary = BrandColors.Accent,
    onPrimary = Color.White,
    primaryContainer = BrandColors.AccentWash,
    onPrimaryContainer = BrandColors.AccentDark,
    secondary = BrandColors.Ink,
    onSecondary = Color.White,
    secondaryContainer = BrandColors.Border,
    onSecondaryContainer = BrandColors.Ink,
    tertiary = Color(0xFF2F6F63),
    onTertiary = Color.White,
    error = BrandColors.Orange,
    onError = Color.White,
    background = BrandColors.Ground,
    onBackground = BrandColors.Ink,
    surface = BrandColors.Card,
    onSurface = BrandColors.Ink,
    surfaceVariant = BrandColors.Ground,
    onSurfaceVariant = BrandColors.Muted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = BrandColors.Card,
    surfaceContainer = BrandColors.Hairline,
    surfaceContainerHigh = BrandColors.Border,
    surfaceContainerHighest = BrandColors.Control,
    inverseSurface = BrandColors.Ink,
    inverseOnSurface = Color.White,
    outline = BrandColors.Control,
    outlineVariant = BrandColors.Border,
)

internal val DarkColors = darkColorScheme(
    primary = BrandColors.AccentSoft,
    onPrimary = Color(0xFF0B1A4A),
    primaryContainer = BrandColors.AccentDark,
    onPrimaryContainer = Color(0xFFDCE4FC),
    secondary = BrandColors.NightText,
    onSecondary = BrandColors.Ink,
    secondaryContainer = BrandColors.NightHigh,
    onSecondaryContainer = BrandColors.NightText,
    tertiary = Color(0xFF7FC8B6),
    onTertiary = Color(0xFF0E2E28),
    error = Color(0xFFF2A37A),
    onError = Color(0xFF3D1606),
    background = BrandColors.NightGround,
    onBackground = BrandColors.NightText,
    surface = BrandColors.NightCard,
    onSurface = BrandColors.NightText,
    surfaceVariant = BrandColors.NightContainer,
    onSurfaceVariant = BrandColors.NightMuted,
    surfaceContainerLowest = BrandColors.NightGround,
    surfaceContainerLow = BrandColors.NightCard,
    surfaceContainer = BrandColors.NightContainer,
    surfaceContainerHigh = BrandColors.NightHigh,
    surfaceContainerHighest = BrandColors.NightControl,
    inverseSurface = Color(0xFF1E2A4A),
    inverseOnSurface = Color.White,
    outline = BrandColors.NightControl,
    outlineVariant = BrandColors.NightBorder,
)

/** Semantic colours Material 3 has no slot for: money in/out, warnings, and the dark hero card. */
data class FinanceColors(
    val positive: Color,
    val negative: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val warningOutline: Color,
    val goodContainer: Color,
    val onGoodContainer: Color,
    val heroContainer: Color,
    val onHeroContainer: Color,
    val heroMuted: Color,
    val heroTrack: Color,
    val heroAccent: Color,
    val selectedChip: Color,
    val onSelectedChip: Color,
    val chartMuted: Color,
)

internal val LightFinanceColors = FinanceColors(
    positive = Color(0xFF2F6F63),
    negative = BrandColors.Orange,
    warningContainer = Color(0xFFFCEBDD),
    onWarningContainer = Color(0xFF6E2C0C),
    warningOutline = Color(0xFFE2B79B),
    goodContainer = Color(0xFFE7F2EF),
    onGoodContainer = Color(0xFF1D4A41),
    heroContainer = BrandColors.Ink,
    onHeroContainer = Color.White,
    heroMuted = Color(0xFFB9C1D3),
    heroTrack = Color(0xFF2A3350),
    heroAccent = BrandColors.AccentSoft,
    selectedChip = BrandColors.Ink,
    onSelectedChip = Color.White,
    chartMuted = Color(0xFFC9D3EE),
)

internal val DarkFinanceColors = FinanceColors(
    positive = Color(0xFF7FC8B6),
    negative = Color(0xFFF2A37A),
    warningContainer = Color(0xFF4A2412),
    onWarningContainer = Color(0xFFFCD9C2),
    warningOutline = Color(0xFF7A3E22),
    goodContainer = Color(0xFF173A33),
    onGoodContainer = Color(0xFFCDEBE3),
    heroContainer = Color(0xFF1E2A4A),
    onHeroContainer = Color.White,
    heroMuted = Color(0xFFB9C1D3),
    heroTrack = Color(0xFF2E3B60),
    heroAccent = BrandColors.AccentSoft,
    selectedChip = BrandColors.NightText,
    onSelectedChip = BrandColors.Ink,
    chartMuted = Color(0xFF3A4766),
)

/** Parses `#RRGGBB` (category colours are user data stored as hex). Falls back to grey. */
fun colorFromHex(hex: String): Color {
    val digits = hex.removePrefix("#")
    val value = digits.toLongOrNull(16)?.takeIf { digits.length == 6 } ?: return Color(0xFFA3ABBA)
    return Color(0xFF000000 or value)
}

/**
 * A category's colour as the current theme shows it. On a dark surface a colour too dark to see
 * (the design's ink, Housing's default) is lifted to the muted text colour; light themes keep it.
 */
@Composable
fun categoryColor(hex: String): Color {
    val color = colorFromHex(hex)
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (darkSurface && color.luminance() < INVISIBLE_ON_DARK) MaterialTheme.colorScheme.onSurfaceVariant else color
}

/** Relative luminance below which a colour disappears on the dark surfaces (ink is about 0.01). */
private const val INVISIBLE_ON_DARK = 0.03f
