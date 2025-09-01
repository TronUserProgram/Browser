package com.skdev.browser

import android.app.Application
import de.cotech.hw.SecurityKeyManager
import de.cotech.hw.SecurityKeyManagerConfig

class MyCustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Hardware Security SDK
        val securityKeyManager = SecurityKeyManager.getInstance()
        val config = SecurityKeyManagerConfig.Builder()
            .setEnableDebugLogging(BuildConfig.DEBUG)
            .build()
        securityKeyManager.init(this, config)
    }
}