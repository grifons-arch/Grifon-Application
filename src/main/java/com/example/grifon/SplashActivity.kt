package com.example.grifon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    @Inject
    lateinit var catalogApi: CatalogApi

    @Inject
    lateinit var getActiveShopUseCase: GetActiveShopUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            runStartupSync()
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }

    private suspend fun runStartupSync() {
        val minSplashDurationMs = 1000L
        val startTs = System.currentTimeMillis()

        updateProgress(10)

        withTimeoutOrNull(10_000L) {
            val activeShopId = getActiveShopUseCase().first().toIntOrNull() ?: 4
            updateProgress(35)

            runCatching {
                catalogApi.getCategories(shopId = activeShopId, pageSize = 80)
            }
            updateProgress(65)

            runCatching {
                catalogApi.getCategoryProducts(
                    categoryId = 2,
                    shopId = activeShopId,
                    pageSize = 40
                )
            }
            updateProgress(100)
        }

        val elapsed = System.currentTimeMillis() - startTs
        if (elapsed < minSplashDurationMs) {
            delay(minSplashDurationMs - elapsed)
        }
    }

    private fun updateProgress(value: Int) {
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.splash_progress)
        progressBar.progress = value.coerceIn(0, 100)
    }
}
