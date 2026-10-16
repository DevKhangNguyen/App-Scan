package com.example

import android.app.Application
import com.example.util.AdMobManager

class ScanDocApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob safely in background (offline resilient)
        AdMobManager.initialize(this)
    }
}
