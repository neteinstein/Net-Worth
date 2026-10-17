package com.neteinstein.networth.feature.future.ui.screens

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.testing.screenshot
import com.neteinstein.networth.feature.future.domain.model.FutureScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    @Test
    fun future() = screenshot("future") { FutureContent(previewFutureState(), {}, {}, {}, {}, {}, {}) }

    @Test
    fun futureKids() = screenshot("future_kids") { FutureContent(previewFutureState(FutureScope.KIDS), {}, {}, {}, {}, {}, {}) }

    @Test
    fun futureEmpty() = screenshot("future_empty") {
        FutureContent(FutureUiState(future = previewFutureState().future!!.copy(latest = null)), {}, {}, {}, {}, {}, {})
    }

    @Test
    fun yearAhead() = screenshot("year_ahead") { YearAheadContent(previewYearAheadState(), {}, {}) }

    @Test
    fun yearAheadAccount() = screenshot("year_ahead_account") { YearAheadContent(previewYearAheadState(AccountScope.Single("a1")), {}, {}) }
}
