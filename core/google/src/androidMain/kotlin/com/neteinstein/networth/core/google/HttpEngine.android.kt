package com.neteinstein.networth.core.google

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

internal actual val platformEngine: HttpClientEngineFactory<*> = OkHttp
