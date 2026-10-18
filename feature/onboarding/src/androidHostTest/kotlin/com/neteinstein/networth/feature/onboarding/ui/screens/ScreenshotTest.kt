package com.neteinstein.networth.feature.onboarding.ui.screens

import com.neteinstein.networth.core.domain.repository.DatabaseFolder
import com.neteinstein.networth.core.domain.repository.UserProfile
import com.neteinstein.networth.core.testing.screenshot
import com.neteinstein.networth.feature.onboarding.domain.model.HouseholdForm
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    @Test
    fun signIn() = screenshot("sign_in") { OnboardingContent(OnboardingUiState(), OnboardingActions()) }

    @Test
    fun choose() = screenshot("choose") {
        OnboardingContent(OnboardingUiState(OnboardingStep.CHOOSE, UserProfile("1", "pedro@example.com", "Pedro")), OnboardingActions())
    }

    @Test
    fun startFresh() = screenshot("start_fresh") {
        OnboardingContent(
            OnboardingUiState(
                step = OnboardingStep.START_FRESH,
                form = HouseholdForm("The Vicentes", listOf("Pedro", "Maria"), listOf("Inês", "")),
            ),
            OnboardingActions(),
        )
    }

    @Test
    fun startFreshChooseFile() = screenshot("start_fresh_choose_file") {
        OnboardingContent(
            OnboardingUiState(
                step = OnboardingStep.START_FRESH,
                form = HouseholdForm(
                    householdName = "The Vicentes",
                    adults = listOf("Pedro"),
                    chooseFile = true,
                    fileName = "Our money",
                    folder = DatabaseFolder("folder-1", "Finances"),
                ),
            ),
            OnboardingActions(),
        )
    }
}
