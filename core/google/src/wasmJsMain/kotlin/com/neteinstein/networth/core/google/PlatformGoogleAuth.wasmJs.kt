package com.neteinstein.networth.core.google

import org.koin.core.scope.Scope

actual fun Scope.googleAuthPlatform(): GoogleAuthPlatform = WebGoogleAuth(googleHttpClient(), ProfileStore(get()))
