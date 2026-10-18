package com.neteinstein.networth.core.designsystem.ui.files

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** Shared files are written here (cache, see res/xml/networth_shared_files.xml) and served by a FileProvider. */
private const val SHARED_DIR = "shared"

@Composable
actual fun rememberFileSharer(): (SharedFile) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { file ->
            val dir = File(context.cacheDir, SHARED_DIR).apply { mkdirs() }
            val target = File(dir, file.name).apply { writeText(file.text) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", target)
            val send =
                Intent(Intent.ACTION_SEND)
                    .setType(file.mimeType)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, file.name)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
