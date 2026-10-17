package com.neteinstein.networth.core.google

import android.content.Context
import org.koin.core.scope.Scope

actual fun Scope.googleAuthPlatform(): GoogleAuthPlatform = AndroidGoogleAuth(get<Context>(), ProfileStore(get()))
