package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.repository.LocalStore
import org.koin.core.scope.Scope

/** This platform's [LocalStore] (Android needs the application Context from Koin). */
expect fun Scope.platformLocalStore(): LocalStore
