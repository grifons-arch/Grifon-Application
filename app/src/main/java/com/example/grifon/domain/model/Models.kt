package com.example.grifon.domain.model

data class Shop(
    val id: String,
    val name: String,
)

data class Category(
    val id: String,
    val name: String,
    val parentId: String?,
    val childrenCount: Int,
    val position: Int? = null,
    val slug: String? = null,
)

data class CatalogFacet(
    val key: String,
    val title: String,
    val type: CatalogFacetType,
    val options: List<CatalogFacetOption> = emptyList(),
    val minValue: Double? = null,
    val maxValue: Double? = null,
)

data class CatalogFacetOption(
    val key: String,
    val label: String,
    val count: Int,
)

enum class CatalogFacetType {
    COLOR,
    BRAND,
    ATTRIBUTE,
    PRICE,
    AVAILABILITY,
}

data class Product(
    val id: String,
    val title: String,
    val price: Double?,
    val currency: String,
    val imageUrl: String,
    val images: List<String> = emptyList(),
    val brand: String,
    val rating: Double,
    val inStock: Boolean,
    val attributesMap: Map<String, List<String>>,
)

data class FavoriteProduct(
    val productId: String,
    val title: String,
    val price: Double?,
    val currency: String,
    val imageUrl: String,
    val brand: String,
    val shopId: String,
)

data class RecentProduct(
    val productId: String,
    val title: String,
    val price: Double?,
    val currency: String,
    val imageUrl: String,
    val brand: String,
    val shopId: String,
)

data class CartItem(
    val productId: String,
    val title: String,
    val productCode: String,
    val imageUrl: String,
    val qty: Int,
    val priceSnapshot: Double,
    val currency: String,
)

data class FilterState(
    val priceRange: ClosedFloatingPointRange<Double> = 0.0..500.0,
    val brands: Set<String> = emptySet(),
    val colors: Set<String> = emptySet(), // Προσθήκη για τα χρώματα
    val inStockOnly: Boolean = false,
    val ratingMin: Double = 0.0,
    val saleOnly: Boolean = false,
    val deliveryOptions: Set<String> = emptySet(),
    val attributes: Map<String, Set<String>> = emptyMap(),
)

enum class SortOption(val label: String) {
    RELEVANCE("Σχετικότητα"),
    PRICE_LOW_HIGH("Τιμή ↑"),
    PRICE_HIGH_LOW("Τιμή ↓"),
    RATING("Rating"),
}
