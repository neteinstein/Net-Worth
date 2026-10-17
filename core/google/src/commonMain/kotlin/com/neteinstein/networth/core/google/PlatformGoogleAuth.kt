package com.neteinstein.networth.core.google

import org.koin.core.scope.Scope

/**
 * Builds this platform's [GoogleAuthPlatform] from Koin (Android needs the application Context).
 * Bound in `AppModule` with `createdAtStart = true` so Android can track the foreground Activity
 * from the very first screen.
 */
expect fun Scope.googleAuthPlatform(): GoogleAuthPlatform
