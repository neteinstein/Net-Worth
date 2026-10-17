package com.neteinstein.networth.feature.settings.ui.screens

import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.repository.DatabaseFile
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.core.testing.screenshot
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    @Test
    fun settings() = screenshot("settings") {
        SettingsContent(
            state = SettingsUiState(
                appearance = AppearanceSettings(ThemeMode.SYSTEM, useDynamicColor = false),
                dynamicColorAvailable = true,
                email = "pedro@example.com",
                file = DatabaseFile("f", "Net Worth – The Vicentes.networth.json"),
                sync = SyncStatus.SYNCED,
                householdName = "The Vicentes",
                members = listOf(Member("p", "Pedro", MemberRole.ADULT), Member("i", "Inês", MemberRole.CHILD)),
            ),
            actions = SettingsActions(),
        )
    }
}
