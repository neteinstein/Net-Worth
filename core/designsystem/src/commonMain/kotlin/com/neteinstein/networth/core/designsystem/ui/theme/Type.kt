package com.neteinstein.networth.core.designsystem.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.neteinstein.networth.core.designsystem.generated.resources.Res
import com.neteinstein.networth.core.designsystem.generated.resources.bricolage_grotesque_bold
import com.neteinstein.networth.core.designsystem.generated.resources.bricolage_grotesque_medium
import com.neteinstein.networth.core.designsystem.generated.resources.ibm_plex_mono_medium
import com.neteinstein.networth.core.designsystem.generated.resources.ibm_plex_mono_semibold
import com.neteinstein.networth.core.designsystem.generated.resources.instrument_sans_bold
import com.neteinstein.networth.core.designsystem.generated.resources.instrument_sans_medium
import com.neteinstein.networth.core.designsystem.generated.resources.instrument_sans_regular
import com.neteinstein.networth.core.designsystem.generated.resources.instrument_sans_semibold
import org.jetbrains.compose.resources.Font

/** Bricolage Grotesque: big figures and screen titles. */
@Composable
internal fun displayFamily() = FontFamily(
    Font(Res.font.bricolage_grotesque_medium, FontWeight.Medium),
    Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold),
)

/** Instrument Sans: everything else. */
@Composable
internal fun bodyFamily() = FontFamily(
    Font(Res.font.instrument_sans_regular, FontWeight.Normal),
    Font(Res.font.instrument_sans_medium, FontWeight.Medium),
    Font(Res.font.instrument_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.instrument_sans_bold, FontWeight.Bold),
)

/** IBM Plex Mono: raw bank descriptions, shown exactly as the bank wrote them. */
@Composable
internal fun monoFamily() = FontFamily(
    Font(Res.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(Res.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

/** Sizes follow the artboards: 54/40/34 px hero figures, 28 px titles, 16 px section titles, 14 px body. */
@Composable
internal fun netWorthTypography(): Typography {
    val display = displayFamily()
    val body = bodyFamily()
    // Figures line up in columns ("font-variant-numeric: tabular-nums" in the design).
    val tabular = "tnum"

    fun displayStyle(size: Int, weight: FontWeight = FontWeight.Bold, tracking: Double = -0.02) = TextStyle(
        fontFamily = display,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (size * 1.1).sp,
        letterSpacing = tracking.em,
        fontFeatureSettings = tabular,
    )

    fun bodyStyle(size: Int, weight: FontWeight, lineHeight: Double = 1.4) = TextStyle(
        fontFamily = body,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (size * lineHeight).sp,
        fontFeatureSettings = tabular,
    )
    return Typography(
        displayLarge = displayStyle(54, tracking = -0.03),
        displayMedium = displayStyle(40, tracking = -0.03),
        displaySmall = displayStyle(34),
        headlineLarge = displayStyle(28),
        headlineMedium = displayStyle(26),
        headlineSmall = displayStyle(22),
        titleLarge = bodyStyle(18, FontWeight.SemiBold),
        titleMedium = bodyStyle(16, FontWeight.Bold),
        titleSmall = bodyStyle(15, FontWeight.Bold),
        bodyLarge = bodyStyle(15, FontWeight.Normal, 1.45),
        bodyMedium = bodyStyle(14, FontWeight.Normal, 1.45),
        bodySmall = bodyStyle(13, FontWeight.Normal, 1.4),
        labelLarge = bodyStyle(14, FontWeight.SemiBold),
        labelMedium = bodyStyle(13, FontWeight.SemiBold),
        labelSmall = bodyStyle(12, FontWeight.Medium),
    )
}
