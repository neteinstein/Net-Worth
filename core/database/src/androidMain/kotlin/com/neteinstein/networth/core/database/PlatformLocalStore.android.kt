package com.neteinstein.networth.core.database

import android.content.Context
import com.neteinstein.networth.core.domain.repository.LocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.scope.Scope
import java.io.File

actual fun Scope.platformLocalStore(): LocalStore = FileLocalStore(File(get<Context>().filesDir, "networth"))

/** Files in the app's private storage (excluded from backups — see data_extraction_rules.xml). */
internal class FileLocalStore(private val directory: File) : LocalStore {
    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        File(directory, name).takeIf { it.exists() }?.readText()
    }

    override suspend fun write(name: String, text: String) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val target = File(directory, name)
        val temp = File(directory, "$name.tmp")
        temp.writeText(text)
        if (!temp.renameTo(target)) {
            target.writeText(text)
            temp.delete()
        }
    }

    override suspend fun delete(name: String) {
        withContext(Dispatchers.IO) { File(directory, name).delete() }
    }
}
