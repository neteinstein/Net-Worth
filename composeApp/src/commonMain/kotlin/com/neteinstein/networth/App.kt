package com.neteinstein.networth

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.locale.AppLocaleEnvironment
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.feature.onboarding.ui.screens.OnboardingScreen
import com.neteinstein.networth.ui.AppViewModel
import com.neteinstein.networth.ui.RootDestination
import com.neteinstein.networth.ui.navigation.AppNavigation
import com.neteinstein.networth.ui.navigation.rememberAppBackStack
import com.neteinstein.networth.ui.navigation.resetToOverview
import org.koin.compose.viewmodel.koinViewModel

/** Root composable shared by every platform. Koin must already be started (see `initKoin`). */
@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    // Held above the language key so switching language in Settings keeps the user on Settings.
    val backStack = rememberAppBackStack()
    LaunchedEffect(destination) { if (destination == RootDestination.ONBOARDING) backStack.resetToOverview() }
    NetWorthTheme(
        darkTheme = appearance.isDark(systemIsDark = isSystemInDarkTheme()),
        dynamicColor = appearance.useDynamicColor,
    ) {
        AppLocaleEnvironment(appearance.language.tag) {
            Crossfade(destination, Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { target ->
                when (target) {
                    RootDestination.LOADING -> LoadingState()
                    RootDestination.ONBOARDING -> OnboardingScreen()
                    RootDestination.MAIN -> AppNavigation(backStack)
                }
            }
        }
    }
}
