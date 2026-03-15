package com.example.grifon.data.catalog

import com.example.grifon.domain.model.Product
import javax.inject.Inject
import com.example.grifon.BuildConfig
import android.util.Log

class HomeProductsWebService @Inject constructor(
    private val catalogApi: CatalogApi,
) {
    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    suspend fun fetchProductsForShop(shopKey: String): List<Product> {
        val shops = catalogApi.getShops()
        val selectedShop = resolveShop(shops, shopKey) ?: throw Exception("Shop not found: $shopKey")

        Log.d("GrifonAPI", "Fetching products for shop: ${selectedShop.id} (${selectedShop.code})")
        
        // Προσπάθεια για γενικά προϊόντα
        val productsResponse = catalogApi.getProducts(shopId = selectedShop.id, pageSize = 50)
        
        if (productsResponse.items.isNotEmpty()) {
            return productsResponse.items.map { dto ->
                dto.toDomainProduct(
                    gatewayBaseUrl = gatewayBaseUrl,
                    brand = selectedShop.code ?: "Grifon",
                )
            }
        }

        // Fallback στην κατηγορία 2 (Home) αν τα γενικά είναι άδεια
        Log.d("GrifonAPI", "No products in general list, trying category 2")
        val fallbackResponse = catalogApi.getCategoryProducts(
            categoryId = 2,
            shopId = selectedShop.id,
            pageSize = 50,
        )
        
        return fallbackResponse.items.map { dto ->
            dto.toDomainProduct(
                gatewayBaseUrl = gatewayBaseUrl,
                brand = selectedShop.code ?: "Grifon",
            )
        }
    }

    private fun resolveShop(shops: List<ShopDto>, shopKey: String): ShopDto? {
        val normalizedKey = shopKey.trim()
        val asNumericId = normalizedKey.toIntOrNull()

        return shops.firstOrNull { it.id == asNumericId }
            ?: shops.firstOrNull { it.code.equals(normalizedKey, ignoreCase = true) }
            ?: shops.firstOrNull { "shop_${it.code}".equals(normalizedKey, ignoreCase = true) }
            ?: shops.firstOrNull { "shop_${it.id}".equals(normalizedKey, ignoreCase = true) }
            ?: shops.firstOrNull()
    }
}
