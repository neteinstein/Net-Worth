package com.neteinstein.networth.core.google

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

internal actual val platformEngine: HttpClientEngineFactory<*> = Js
