package com.example.grifon.data.catalog

import android.util.Log
import com.example.grifon.domain.model.Product
import com.example.grifon.core.ShopConfig
import javax.inject.Inject
import com.example.grifon.BuildConfig
import com.example.grifon.data.local.LocalPriceAccessService
import com.example.grifon.data.local.ShopPreferences
import kotlinx.coroutines.flow.first

class HomeProductsWebService @Inject constructor(
    private val catalogApi: CatalogApi,
    private val shopPreferences: ShopPreferences,
    private val localPriceAccessService: LocalPriceAccessService,
) {
    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    suspend fun fetchProductsForShop(shopKey: String): List<Product> {
        val shops = catalogApi.getShops()
        val selectedShop = resolveShop(shops, shopKey) ?: throw Exception("Shop not found: $shopKey")

        Log.d("GrifonAPI", "Fetching products for shop: ${selectedShop.id} (${selectedShop.code})")
        val customerId = shopPreferences.currentCustomerId.first()
        val canDisplayPrices = localPriceAccessService.canDisplayPrices(
            shopId = selectedShop.id.toString(),
            customerId = customerId,
            canViewPrices = shopPreferences.canViewPrices.first(),
        )
        val requestCustomerId = customerId?.takeIf { canDisplayPrices }
        
        // Προσπάθεια για γενικά προϊόντα
        val productsResponse = catalogApi.getProducts(
            shopId = selectedShop.id,
            pageSize = 50,
            customerId = requestCustomerId,
        )
        
        if (productsResponse.items.isNotEmpty()) {
            return productsResponse.items.map { dto ->
                dto.toDomainProduct(
                    gatewayBaseUrl = gatewayBaseUrl,
                    brand = selectedShop.code ?: "Grifon",
                    showPrice = canDisplayPrices,
                )
            }
        }

        // Fallback στην κατηγορία 2 (Home) αν τα γενικά είναι άδεια
        Log.d("GrifonAPI", "No products in general list, trying category 2")
        val fallbackResponse = catalogApi.getCategoryProducts(
            categoryId = 2,
            shopId = selectedShop.id,
            pageSize = 50,
            customerId = requestCustomerId,
        )
        
        return fallbackResponse.items.map { dto ->
            dto.toDomainProduct(
                gatewayBaseUrl = gatewayBaseUrl,
                brand = selectedShop.code ?: "Grifon",
                showPrice = canDisplayPrices,
            )
        }
    }

    private fun resolveShop(shops: List<ShopDto>, shopKey: String): ShopDto? {
        val normalizedKey = ShopConfig.normalizeShopId(shopKey)
        val asNumericId = normalizedKey.toInt()

        return shops.firstOrNull { it.id == asNumericId }
            ?: shops.firstOrNull { it.code.equals(normalizedKey, ignoreCase = true) }
            ?: shops.firstOrNull { "shop_${it.id}".equals(normalizedKey, ignoreCase = true) }
            ?: shops.firstOrNull()
    }
}
