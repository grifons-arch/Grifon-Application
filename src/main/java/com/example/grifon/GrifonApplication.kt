package com.example.grifon

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GrifonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("CrashLog", "Application onCreate: Grifon eShop starting...")
    }
}
