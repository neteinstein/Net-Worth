package com.neteinstein.networth.core.designsystem.ui.files

import androidx.compose.runtime.Composable

/** A file chosen in the platform's file picker. */
class PickedFile(
    val name: String,
    val bytes: ByteArray,
)

/** What the picker offers: bank statements (several at once) or one JSON file. */
enum class FileKind(val multiple: Boolean) {
    STATEMENTS(multiple = true),
    JSON(multiple = false),
}

/**
 * Returns a function that opens the platform's file picker for [kind]. [onPicked] gets the chosen files' names
 * and contents; [onFailed] is called if any of them can't be read. Cancelling calls neither.
 */
@Composable
expect fun rememberFilePicker(
    kind: FileKind,
    onPicked: (files: List<PickedFile>) -> Unit,
    onFailed: () -> Unit,
): () -> Unit
