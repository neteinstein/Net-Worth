package com.neteinstein.networth.feature.settings.ui.screens

import com.neteinstein.networth.core.designsystem.ui.locale.AppLocaleEnvironment
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.repository.DatabaseFile
import com.neteinstein.networth.core.domain.repository.SyncStatus
import com.neteinstein.networth.core.testing.screenshot
import com.neteinstein.networth.feature.settings.domain.model.AppLanguage
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    @Test
    fun settings() = screenshot("settings") {
        SettingsContent(state = state(AppLanguage.SYSTEM), actions = SettingsActions())
    }

    /** Tall enough to reach the Claude section at the bottom: the key field, then a saved key. */
    @Test
    fun settingsClaude() {
        screenshot("settings_claude", heightDp = 1800) {
            SettingsContent(state = state(AppLanguage.SYSTEM), actions = SettingsActions())
        }
        screenshot("settings_claude_saved", heightDp = 1700) {
            SettingsContent(state = state(AppLanguage.SYSTEM).copy(claudeKeyHint = "sk-ant-…x7Kq"), actions = SettingsActions())
        }
    }

    /** The whole app is translated; this checks the Portuguese strings load when the language is switched. */
    @Test
    fun settingsInPortuguese() {
        val original = Locale.getDefault()
        try {
            screenshot("settings_pt") {
                AppLocaleEnvironment(AppLanguage.PORTUGUESE.tag) {
                    SettingsContent(state = state(AppLanguage.PORTUGUESE), actions = SettingsActions())
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun state(language: AppLanguage) = SettingsUiState(
        appearance = AppearanceSettings(ThemeMode.SYSTEM, useDynamicColor = false, language = language),
        dynamicColorAvailable = true,
        email = "pedro@example.com",
        file = DatabaseFile("f", "Net Worth – The Vicentes.networth.json"),
        sync = SyncStatus.SYNCED,
        householdName = "The Vicentes",
        members = listOf(Member("p", "Pedro", MemberRole.ADULT), Member("i", "Inês", MemberRole.CHILD)),
    )
}
