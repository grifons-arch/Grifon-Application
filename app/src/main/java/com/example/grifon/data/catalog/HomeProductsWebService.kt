package com.example.grifon.data.catalog

import com.example.grifon.domain.model.Product
import javax.inject.Inject
import com.example.grifon.BuildConfig

class HomeProductsWebService @Inject constructor(
    private val catalogApi: CatalogApi,
) {
    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    suspend fun fetchProductsForShop(shopKey: String): List<Product> {
        try {
            val shops = catalogApi.getShops()
            val selectedShop = resolveShop(shops, shopKey) ?: return emptyList()

            // Αντί για το γενικό getProducts, χρησιμοποιούμε την κατηγορία 2 (Home) 
            // που είναι πιο σίγουρο ότι επιστρέφει αποτελέσματα στο PrestaShop
            val response = catalogApi.getCategoryProducts(categoryId = 2, shopId = selectedShop.id, pageSize = 50)
            
            return response.items.map { it.toDomain(selectedShop.id, selectedShop.code) }
        } catch (e: Exception) {
            return emptyList()
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

    private fun ProductDto.toDomain(shopId: Int, shopCode: String?): Product {
        val normalizedShopCode = shopCode ?: "SHOP"
        
        val rawUrl = defaultImage?.url ?: ""
        val fullImageUrl = if (rawUrl.startsWith("/")) {
            "$gatewayBaseUrl$rawUrl"
        } else {
            rawUrl
        }

        return Product(
            id = "${shopId}_$id",
            title = name ?: "Προϊόν #$id",
            price = price ?: 0.0,
            currency = "EUR",
            imageUrl = fullImageUrl,
            brand = normalizedShopCode,
            rating = 0.0,
            inStock = true,
            attributesMap = mapOf("reference" to (reference ?: "")),
        )
    }
}
