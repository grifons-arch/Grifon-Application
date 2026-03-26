package com.example.grifon

import android.app.Application
import android.content.Context
import com.example.grifon.core.AppLanguage
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GrifonApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrapContext(base))
    }

    override fun onCreate() {
        AppLanguage.apply(AppLanguage.getStoredLanguage(this))
        super.onCreate()
    }
}
