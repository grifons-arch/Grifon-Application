package com.example.grifon.data.catalog

import com.example.grifon.domain.model.Product
import javax.inject.Inject

class HomeProductsWebService @Inject constructor(
    private val catalogApi: CatalogApi,
) {
    suspend fun fetchAllProductsFromShops(): List<Product> {
        val shops = runCatching { catalogApi.getShops() }
            .getOrDefault(listOf(ShopDto(id = 4, code = "GR"), ShopDto(id = 1, code = "SE")))

        val allProducts = mutableListOf<Product>()

        shops.forEach { shop ->
            val categories = runCatching { catalogApi.getCategories(shopId = shop.id).items }
                .getOrDefault(emptyList())

            categories.forEach { category ->
                val categoryProducts = runCatching {
                    catalogApi.getCategoryProducts(categoryId = category.id, shopId = shop.id).items
                }.getOrDefault(emptyList())
                    .map { it.toDomain(shop.id, shop.code) }

                allProducts += categoryProducts
            }
        }

        return allProducts.distinctBy { it.id }
    }

    private fun ProductDto.toDomain(shopId: Int, shopCode: String?): Product {
        val normalizedShopCode = shopCode ?: "SHOP"
        return Product(
            id = "${shopId}_$id",
            title = name ?: "Προϊόν #$id",
            price = price ?: 0.0,
            currency = "EUR",
            imageUrl = "",
            brand = normalizedShopCode,
            rating = 0.0,
            inStock = true,
            attributesMap = mapOf("reference" to (reference ?: "")),
        )
    }
}
