package com.neteinstein.networth

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.neteinstein.networth.di.initKoin
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import kotlin.time.Clock

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    // Strings are read by byte offset from .cvr files whose URLs never change, and GitHub Pages lets browsers cache
    // them for 10 minutes: after a deploy, a cached file from another build shows slices of the wrong strings.
    // Fetch them fresh on every start (index.html does the same for composeApp.js, so code and strings match).
    val start = Clock.System.now().toEpochMilliseconds()
    configureWebResources {
        resourcePathMapping { path -> if (path.endsWith(".cvr")) "./$path?v=$start" else "./$path" }
    }
    initKoin()
    ComposeViewport { App() }
}
