package com.neteinstein.networth.core.google

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual val platformEngine: HttpClientEngineFactory<*> = Darwin
