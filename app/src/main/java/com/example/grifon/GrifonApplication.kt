package com.example.grifon

import android.app.Application
import android.content.Context
import com.example.grifon.core.AppLanguage
import com.example.grifon.data.sync.WholesaleCustomerStartupSyncer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GrifonApplication : Application() {
    @Inject
    lateinit var wholesaleCustomerStartupSyncer: WholesaleCustomerStartupSyncer

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrapContext(base))
    }

    override fun onCreate() {
        AppLanguage.apply(AppLanguage.getStoredLanguage(this))
        super.onCreate()
        wholesaleCustomerStartupSyncer.syncOnAppLaunch()
    }
}
