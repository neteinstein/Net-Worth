package com.neteinstein.networth.core.google

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory

/** Named per platform (OkHttp, Darwin, browser fetch) instead of ServiceLoader discovery, which R8 can strip. */
internal expect val platformEngine: HttpClientEngineFactory<*>

fun googleHttpClient(): HttpClient = HttpClient(platformEngine) {
    expectSuccess = false
}
