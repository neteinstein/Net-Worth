@file:OptIn(ExperimentalWasmJsInterop::class)

package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.repository.LocalStore
import org.koin.core.scope.Scope

actual fun Scope.platformLocalStore(): LocalStore = BrowserLocalStore()

/** `localStorage` (about 5 MB per origin — enough for years of transactions). */
internal class BrowserLocalStore : LocalStore {
    override suspend fun read(name: String): String? = storageGet(KEY_PREFIX + name)

    override suspend fun write(name: String, text: String) = storageSet(KEY_PREFIX + name, text)

    override suspend fun delete(name: String) = storageRemove(KEY_PREFIX + name)

    private companion object {
        const val KEY_PREFIX = "networth:"
    }
}

@JsFun("(key) => window.localStorage.getItem(key)")
private external fun storageGet(key: String): String?

@JsFun("(key, value) => window.localStorage.setItem(key, value)")
private external fun storageSet(key: String, value: String)

@JsFun("(key) => window.localStorage.removeItem(key)")
private external fun storageRemove(key: String)
