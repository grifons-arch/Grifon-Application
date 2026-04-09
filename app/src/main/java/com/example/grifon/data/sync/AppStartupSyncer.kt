package com.example.grifon.data.sync

import android.util.Log
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.usecase.SyncCustomersUseCase
import com.example.grifon.domain.usecase.SyncProductsUseCase
import com.example.grifon.domain.usecase.SyncWholesaleCustomersUseCase
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class AppStartupSyncer @Inject constructor(
    private val syncWholesaleCustomersUseCase: SyncWholesaleCustomersUseCase,
    private val syncCustomersUseCase: SyncCustomersUseCase,
    private val syncProductsUseCase: SyncProductsUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hasStarted = AtomicBoolean(false)

    fun syncOnAppLaunch() {
        if (!hasStarted.compareAndSet(false, true)) {
            return
        }

        scope.launch {
            listOf(ShopConfig.GreekShopId, ShopConfig.SwedishShopId).forEach { shopId ->
                syncStep("wholesale customers", shopId) { syncWholesaleCustomersUseCase(shopId) }
                syncStep("customers", shopId) { syncCustomersUseCase(shopId) }
                syncStep("products", shopId) { syncProductsUseCase(shopId) }
            }
        }
    }

    private suspend fun syncStep(
        label: String,
        shopId: String,
        block: suspend () -> Int,
    ) {
        runCatching { block() }
            .onFailure { error ->
                Log.w(
                    "AppStartupSync",
                    "Failed to sync $label for shop $shopId",
                    error,
                )
            }
    }
}
