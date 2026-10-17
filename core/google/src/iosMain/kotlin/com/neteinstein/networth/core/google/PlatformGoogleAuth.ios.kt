package com.neteinstein.networth.core.google

import org.koin.core.scope.Scope

actual fun Scope.googleAuthPlatform(): GoogleAuthPlatform = IosGoogleAuth(
    googleHttpClient(),
    ProfileStore(get()),
    KeychainStore("com.neteinstein.networth.google"),
)
