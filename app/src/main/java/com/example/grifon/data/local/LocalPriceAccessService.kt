package com.example.grifon.data.local

import com.example.grifon.core.ShopConfig
import com.example.grifon.data.repository.WholesaleCustomerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class LocalPriceAccessService @Inject constructor(
    private val wholesaleCustomerRepository: WholesaleCustomerRepository,
) {
    fun observeCanDisplayPrices(
        shopId: String,
        customerId: Int?,
        canViewPrices: Boolean,
    ): Flow<Boolean> {
        if (customerId == null) {
            return flowOf(false)
        }

        // The authenticated session flag is the authoritative source for immediate price access.
        // Local wholesale cache can lag behind right after login, so it must not block the UI.
        if (canViewPrices) {
            return flowOf(true)
        }

        return wholesaleCustomerRepository.observeIsWholesaleCustomer(
            shopId = ShopConfig.normalizeShopId(shopId),
            customerId = customerId,
        )
    }

    suspend fun canDisplayPrices(
        shopId: String,
        customerId: Int?,
        canViewPrices: Boolean,
    ): Boolean {
        if (customerId == null) {
            return false
        }

        if (canViewPrices) {
            return true
        }

        return wholesaleCustomerRepository.isWholesaleCustomer(
            shopId = ShopConfig.normalizeShopId(shopId),
            customerId = customerId,
        )
    }
}
