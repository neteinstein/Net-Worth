package com.neteinstein.networth.core.designsystem.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

actual object LocalAppLocale {
    private const val LANG_KEY = "AppleLanguages"
    private val default = NSLocale.preferredLanguages.first() as String
    private val localAppLocale = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = localAppLocale.current

    @Composable
    actual infix fun provides(tag: String?): ProvidedValue<*> {
        val defaults = NSUserDefaults.standardUserDefaults
        if (tag == null) defaults.removeObjectForKey(LANG_KEY) else defaults.setObject(listOf(tag), LANG_KEY)
        return localAppLocale.provides(tag ?: default)
    }
}
