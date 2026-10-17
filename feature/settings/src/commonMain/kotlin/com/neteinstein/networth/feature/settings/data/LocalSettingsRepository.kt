package com.neteinstein.networth.feature.settings.data

import com.neteinstein.networth.core.domain.repository.LocalStore
import com.neteinstein.networth.feature.settings.domain.model.AppearanceSettings
import com.neteinstein.networth.feature.settings.domain.model.ThemeMode
import com.neteinstein.networth.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Appearance is a per-device preference (one parent may like dark mode), so it is kept on this
 * device in [LocalStore], not in the shared family database.
 */
class LocalSettingsRepository(private val store: LocalStore) : SettingsRepository {
    private val appearance = MutableStateFlow<AppearanceSettings?>(null)
    private val lock = Mutex()

    override fun observeAppearance(): Flow<AppearanceSettings> = flow {
        loaded()
        emitAll(appearance.filterNotNull())
    }

    override suspend fun setThemeMode(mode: ThemeMode) = save { it.copy(themeMode = mode) }

    override suspend fun setDynamicColor(enabled: Boolean) = save { it.copy(useDynamicColor = enabled) }

    private suspend fun loaded(): AppearanceSettings = lock.withLock {
        appearance.value ?: decode(store.read(ENTRY)).also { appearance.value = it }
    }

    private suspend fun save(change: (AppearanceSettings) -> AppearanceSettings) {
        val updated = change(loaded())
        lock.withLock {
            appearance.value = updated
            store.write(ENTRY, "themeMode=${updated.themeMode.name}\ndynamicColor=${updated.useDynamicColor}")
        }
    }

    private fun decode(text: String?): AppearanceSettings {
        val values = text.orEmpty().lines().associate { it.substringBefore('=') to it.substringAfter('=', "") }
        return AppearanceSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == values["themeMode"] } ?: ThemeMode.SYSTEM,
            useDynamicColor = values["dynamicColor"] == "true",
        )
    }

    private companion object {
        const val ENTRY = "appearance.txt"
    }
}
