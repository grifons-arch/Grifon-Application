package com.example.grifon.data.catalog

import com.example.grifon.domain.model.Product
import javax.inject.Inject

class HomeProductsWebService @Inject constructor(
    private val catalogApi: CatalogApi,
) {
    suspend fun fetchProductsForShop(shopKey: String): List<Product> {
        val shops = catalogApi.getShops()
        val selectedShop = resolveShop(shops, shopKey) ?: return emptyList()

        // Πλέον φέρνουμε όλα τα προϊόντα του καταστήματος (όχι μόνο μιας κατηγορίας)
        val response = catalogApi.getProducts(shopId = selectedShop.id, pageSize = 50)
        
        return response.items.map { it.toDomain(selectedShop.id, selectedShop.code) }
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
        return Product(
            id = "${shopId}_$id",
            title = name ?: "Προϊόν #$id",
            price = price ?: 0.0,
            currency = "EUR",
            imageUrl = defaultImage?.url ?: "",
            brand = normalizedShopCode,
            rating = 0.0,
            inStock = true,
            attributesMap = mapOf("reference" to (reference ?: "")),
        )
    }
}
