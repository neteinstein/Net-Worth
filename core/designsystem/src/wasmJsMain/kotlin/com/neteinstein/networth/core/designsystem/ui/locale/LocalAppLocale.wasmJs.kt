@file:OptIn(ExperimentalWasmJsInterop::class)

package com.neteinstein.networth.core.designsystem.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

actual object LocalAppLocale {
    private val localAppLocale = staticCompositionLocalOf { Locale.current }

    actual val current: String
        @Composable get() = localAppLocale.current.toString()

    @Composable
    actual infix fun provides(tag: String?): ProvidedValue<*> {
        updateCustomLocale(tag?.replace('_', '-'))
        return localAppLocale.provides(Locale.current)
    }
}

/** `index.html` makes `navigator.languages` return `window.__customLocale` when it is set. */
@JsFun(
    """(value) => {
        if (window.__customLocale !== value) {
            window.__customLocale = value;
            window.dispatchEvent(new Event("languagechange"));
        }
    }""",
)
private external fun updateCustomLocale(value: String?)
