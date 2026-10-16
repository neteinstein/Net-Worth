package com.neteinstein.networth

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.ui.AppViewModel
import com.neteinstein.networth.ui.navigation.AppNavigation
import org.koin.compose.viewmodel.koinViewModel

/** Root composable shared by every platform. Koin must already be started (see `initKoin`). */
@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    NetWorthTheme(
        darkTheme = appearance.isDark(systemIsDark = isSystemInDarkTheme()),
        dynamicColor = appearance.useDynamicColor,
    ) {
        AppNavigation()
    }
}
