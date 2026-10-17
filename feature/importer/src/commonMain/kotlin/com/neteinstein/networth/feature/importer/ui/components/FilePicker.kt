package com.neteinstein.networth.feature.importer.ui.components

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens the platform's file picker for bank statements. [onPicked] gets the
 * file name and contents; [onFailed] is called if the file can't be read. Cancelling calls neither.
 */
@Composable
expect fun rememberStatementPicker(
    onPicked: (name: String, bytes: ByteArray) -> Unit,
    onFailed: () -> Unit,
): () -> Unit
