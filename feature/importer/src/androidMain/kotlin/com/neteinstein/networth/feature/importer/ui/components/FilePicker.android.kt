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
    onPicked: (name: String, bytes: ByteArray) -> Unit,
    onFailed: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onFailed)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val file =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val resolver = context.contentResolver
                            val name =
                                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                    if (cursor.moveToFirst()) cursor.getString(0) else null
                                } ?: uri.lastPathSegment.orEmpty()
                            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Unreadable")
                            name to bytes
                        }.getOrNull()
                    }
                if (file == null) failed() else picked(file.first, file.second)
            }
        }
    return { launcher.launch(STATEMENT_TYPES) }
}
