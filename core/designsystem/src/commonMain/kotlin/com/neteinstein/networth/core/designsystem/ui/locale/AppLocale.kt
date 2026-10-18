package com.neteinstein.networth.core.designsystem.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

/**
 * Overrides the locale Compose resources resolve strings with, per platform (see the Compose
 * Multiplatform "resource environment" docs): Android swaps the `Configuration`, iOS sets
 * `AppleLanguages`, the web overrides `navigator.languages` (see `index.html`).
 */
expect object LocalAppLocale {
    val current: String
        @Composable get

    /** [tag] is a BCP 47 language tag such as "pt"; null restores the device's language. */
    @Composable
    infix fun provides(tag: String?): ProvidedValue<*>
}

/** Shows [content] in [languageTag] (null = the device's language), recomposing it all on a change. */
@Composable
fun AppLocaleEnvironment(languageTag: String?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLocale provides languageTag) {
        key(languageTag) { content() }
    }
}
