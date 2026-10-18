package com.neteinstein.networth.feature.overview.ui.screens

import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.core.testing.screenshot
import com.neteinstein.networth.feature.overview.ui.components.previewAllTimeOverview
import com.neteinstein.networth.feature.overview.ui.components.previewOverview
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    @Test
    fun overview() = screenshot("overview") { OverviewContent(OverviewUiState(previewOverview(), SyncStatus.SYNCED), {}, {}, {}, {}, {}) }

    @Test
    fun overviewEmpty() = screenshot("overview_empty") {
        OverviewContent(OverviewUiState(previewOverview().copy(hasTransactions = false), SyncStatus.OFFLINE), {}, {}, {}, {}, {})
    }

    @Test
    fun overviewAllTime() = screenshot("overview_all_time") {
        OverviewContent(OverviewUiState(previewAllTimeOverview(), SyncStatus.SYNCED), {}, {}, {}, {}, {})
    }
}
