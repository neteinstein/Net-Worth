package com.neteinstein.networth.feature.settings.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.SectionHeader
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.generated.resources.Res
import com.neteinstein.networth.feature.settings.generated.resources.settings_appearance
import com.neteinstein.networth.feature.settings.generated.resources.settings_back
import com.neteinstein.networth.feature.settings.generated.resources.settings_dynamic_color
import com.neteinstein.networth.feature.settings.generated.resources.settings_dynamic_color_body
import com.neteinstein.networth.feature.settings.generated.resources.settings_theme
import com.neteinstein.networth.feature.settings.generated.resources.settings_theme_dark
import com.neteinstein.networth.feature.settings.generated.resources.settings_theme_light
import com.neteinstein.networth.feature.settings.generated.resources.settings_theme_system
import com.neteinstein.networth.feature.settings.generated.resources.settings_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        onBack = onBack,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onDynamicColorToggled = viewModel::onDynamicColorToggled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                SectionHeader(stringResource(Res.string.settings_appearance))
                Text(
                    stringResource(Res.string.settings_theme),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.appearance.themeMode == mode,
                            onClick = { onThemeModeSelected(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) {
                            Text(stringResource(mode.label))
                        }
                    }
                }
                AnimatedVisibility(visible = state.dynamicColorAvailable) {
                    ListItem(
                        modifier = Modifier.padding(top = 16.dp),
                        headlineContent = { Text(stringResource(Res.string.settings_dynamic_color)) },
                        supportingContent = { Text(stringResource(Res.string.settings_dynamic_color_body)) },
                        trailingContent = {
                            Switch(
                                checked = state.appearance.useDynamicColor,
                                onCheckedChange = onDynamicColorToggled,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    )
                }
            }
        }
    }
}

private val ThemeMode.label: StringResource
    get() = when (this) {
        ThemeMode.SYSTEM -> Res.string.settings_theme_system
        ThemeMode.LIGHT -> Res.string.settings_theme_light
        ThemeMode.DARK -> Res.string.settings_theme_dark
    }

@Preview
@Composable
private fun SettingsContentPreview() {
    NetWorthTheme {
        SettingsContent(
            state = SettingsUiState(AppearanceSettings(ThemeMode.DARK, useDynamicColor = true), dynamicColorAvailable = true),
            onBack = {},
            onThemeModeSelected = {},
            onDynamicColorToggled = {},
        )
    }
}
