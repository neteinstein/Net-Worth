package com.neteinstein.networth.feature.importer.ui.components

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
actual fun rememberStatementPicker(
    onPicked: (files: List<PickedFile>) -> Unit,
    onFailed: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onFailed)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
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
    return { launcher.launch(STATEMENT_TYPES) }
}
