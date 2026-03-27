package com.example.grifon.data.sync

import android.util.Log
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.usecase.SyncWholesaleCustomersUseCase
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class WholesaleCustomerStartupSyncer @Inject constructor(
    private val syncWholesaleCustomersUseCase: SyncWholesaleCustomersUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hasStarted = AtomicBoolean(false)

    fun syncOnAppLaunch() {
        if (!hasStarted.compareAndSet(false, true)) {
            return
        }

        scope.launch {
            listOf(ShopConfig.GreekShopId, ShopConfig.SwedishShopId).forEach { shopId ->
                runCatching { syncWholesaleCustomersUseCase(shopId) }
                    .onFailure { error ->
                        Log.w(
                            "WholesaleCustomerSync",
                            "Failed to sync wholesale customers for shop $shopId",
                            error,
                        )
                    }
            }
        }
    }
}
