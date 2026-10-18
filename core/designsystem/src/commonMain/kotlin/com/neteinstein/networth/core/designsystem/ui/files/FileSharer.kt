package com.neteinstein.networth.core.designsystem.ui.files

import androidx.compose.runtime.Composable

/**
 * Returns a function that offers a file to the user: the share sheet on Android and iOS (mail, messages, Files,
 * Drive…), a download in the browser.
 */
@Composable
expect fun rememberFileSharer(): (SharedFile) -> Unit
