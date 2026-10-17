package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.repository.LocalStore
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.scope.Scope
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

actual fun Scope.platformLocalStore(): LocalStore = AppSupportLocalStore()

/** Files in Application Support (not visible to the user, not in iCloud Drive). */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class AppSupportLocalStore : LocalStore {
    private val directory: String by lazy {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
        "$base/networth".also { NSFileManager.defaultManager.createDirectoryAtPath(it, true, null, null) }
    }

    override suspend fun read(name: String): String? = NSString.stringWithContentsOfFile("$directory/$name", NSUTF8StringEncoding, null)

    override suspend fun write(name: String, text: String) {
        NSString.create(string = text).writeToFile("$directory/$name", atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    override suspend fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath("$directory/$name", null)
    }
}
