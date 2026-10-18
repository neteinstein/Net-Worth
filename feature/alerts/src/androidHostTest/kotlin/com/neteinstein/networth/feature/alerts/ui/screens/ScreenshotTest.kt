package com.neteinstein.networth.feature.alerts.ui.screens

import com.neteinstein.networth.core.testing.screenshot
import com.neteinstein.networth.feature.alerts.domain.usecase.Alerts
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    @Test
    fun alerts() = screenshot("alerts") { AlertsContent(AlertsUiState(previewAlerts()), {}, {}, { _, _ -> }) }

    @Test
    fun alertsYear() = screenshot("alerts_year") { AlertsContent(AlertsUiState(previewYearAlerts()), {}, {}, { _, _ -> }) }

    @Test
    fun alertsEmpty() = screenshot("alerts_empty") {
        AlertsContent(AlertsUiState(Alerts("EUR", null, 40, emptyList(), emptyList(), emptyList())), {}, {}, { _, _ -> })
    }
}
