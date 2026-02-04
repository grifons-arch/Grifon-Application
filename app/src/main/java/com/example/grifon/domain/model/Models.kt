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
)

data class Product(
    val id: String,
    val title: String,
    val price: Double,
    val currency: String,
    val imageUrl: String,
    val brand: String,
    val rating: Double,
    val inStock: Boolean,
    val attributesMap: Map<String, String>,
)

data class CartItem(
    val productId: String,
    val qty: Int,
    val priceSnapshot: Double,
)

data class FilterState(
    val priceRange: ClosedFloatingPointRange<Double> = 0.0..500.0,
    val brands: Set<String> = emptySet(),
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
