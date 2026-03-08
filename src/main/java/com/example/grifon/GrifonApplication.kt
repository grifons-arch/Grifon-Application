package com.example.grifon

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GrifonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.GOOGLE_MAPS_API_KEY.isNotEmpty()) {
            Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_API_KEY)
        }
    }
}
