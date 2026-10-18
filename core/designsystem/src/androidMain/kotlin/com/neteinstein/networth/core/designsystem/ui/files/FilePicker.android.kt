package com.neteinstein.networth.core.designsystem.ui.files

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FileKind.mimeTypes: Array<String>
    get() = when (this) {
        FileKind.STATEMENTS -> STATEMENT_TYPES
        FileKind.JSON -> arrayOf("application/json", "text/*", "application/octet-stream")
    }

private val STATEMENT_TYPES =
    arrayOf(
        "application/pdf",
        "text/*",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/x-ofx",
        "application/vnd.intu.qfx",
        "application/octet-stream",
    )

@Composable
actual fun rememberFilePicker(
    kind: FileKind,
    onPicked: (files: List<PickedFile>) -> Unit,
    onFailed: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onFailed)
    val read: (List<Uri>) -> Unit = read@{ uris ->
        if (uris.isEmpty()) return@read
        scope.launch {
            val files =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val resolver = context.contentResolver
                        uris.map { uri ->
                            val name =
                                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                    if (cursor.moveToFirst()) cursor.getString(0) else null
                                } ?: uri.lastPathSegment.orEmpty()
                            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Unreadable")
                            PickedFile(name, bytes)
                        }
                    }.getOrNull()
                }
            if (files == null) failed() else picked(files)
        }
    }
    val many = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), read)
    val one = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> read(listOfNotNull(uri)) }
    return { if (kind.multiple) many.launch(kind.mimeTypes) else one.launch(kind.mimeTypes) }
}
