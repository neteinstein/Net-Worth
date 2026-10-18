package com.neteinstein.networth.feature.importer.ui.components

import androidx.compose.runtime.Composable

/** A file chosen in the platform's file picker. */
class PickedFile(
    val name: String,
    val bytes: ByteArray,
)

/**
 * Returns a function that opens the platform's file picker for bank statements; several files can be chosen
 * at once. [onPicked] gets their names and contents; [onFailed] is called if any of them can't be read.
 * Cancelling calls neither.
 */
@Composable
expect fun rememberStatementPicker(
    onPicked: (files: List<PickedFile>) -> Unit,
    onFailed: () -> Unit,
): () -> Unit
