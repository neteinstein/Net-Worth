package com.neteinstein.networth.android

import android.app.Application
import com.google.firebase.FirebaseApp
import com.neteinstein.networth.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class NetWorthApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@NetWorthApplication)
        }
    }
}
