package com.neteinstein.networth.core.designsystem.ui.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    private var default: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(tag: String?): ProvidedValue<*> {
        val configuration = Configuration(LocalConfiguration.current)
        val system = default ?: Locale.getDefault().also { default = it }
        val locale = tag?.let(Locale::forLanguageTag) ?: system
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}
