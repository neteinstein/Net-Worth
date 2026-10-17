package com.neteinstein.networth.core.testing

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import org.robolectric.RuntimeEnvironment

/**
 * Renders [content] on a phone-sized screen in the light and dark theme and compares it with
 * `src/androidHostTest/screenshots/<name>_light.png` / `_dark.png` (recorded with
 * `-PrecordScreenshots`). Call it from a Robolectric test (see any feature's `ScreenshotTest`).
 */
fun screenshot(
    name: String,
    heightDp: Int = SCREEN_HEIGHT_DP,
    content: @Composable () -> Unit,
) {
    RuntimeEnvironment.setQualifiers("w${SCREEN_WIDTH_DP}dp-h${heightDp}dp-hdpi")
    listOf(false to "light", true to "dark").forEach { (dark, suffix) ->
        captureRoboImage(filePath = "src/androidHostTest/screenshots/${name}_$suffix.png", roborazziOptions = options) {
            NetWorthTheme(darkTheme = dark) { content() }
        }
    }
}

/** The artboards' width; tall enough to show most of a screen without scrolling. */
private const val SCREEN_WIDTH_DP = 390
private const val SCREEN_HEIGHT_DP = 1300

/** Small tolerance so anti-aliasing differences between machines don't fail the build. */
private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))
