package com.neteinstein.networth.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * DI bootstrap, called once per process by each platform entry point:
 * - Android: `NetWorthApplication.onCreate()` → `initKoin { androidContext(...) }`
 * - iOS: `iOSApp.init()` → `InitKoinKt.doInitKoin()` (Obj-C export prefixes `init*` with `do`)
 * - Web: `main()` in wasmJsMain
 */
fun initKoin(config: KoinAppDeclaration): KoinApplication = startKoin {
    config(this)
    modules(appModule)
}

/** No-arg overload for Swift (no Kotlin default arguments in Obj-C), returning Unit so Swift needs no `_ =`. */
fun initKoin() {
    initKoin {}
}
