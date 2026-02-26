package com.example.grifon.data.repository

import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    fun getShops(): Flow<List<Shop>>
    fun getActiveShopId(): Flow<String>
    suspend fun setActiveShopId(shopId: String)
}

interface CatalogRepository {
    fun getCategoryTree(shopId: String): Flow<List<Category>>
    fun getProductsByCategory(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption,
    ): Flow<List<Product>>

    fun searchProducts(
        shopId: String,
        query: String,
        filters: FilterState,
        sortOption: SortOption,
    ): Flow<List<Product>>

    fun getProductById(shopId: String, productId: String): Flow<Product?>
    
    suspend fun syncCatalog(shopId: String)
}

interface CartRepository {
    fun observeCart(shopId: String): Flow<List<CartItem>>
    suspend fun addToCart(shopId: String, item: CartItem)
    suspend fun removeFromCart(shopId: String, productId: String)
    suspend fun updateQuantity(shopId: String, productId: String, qty: Int)
}
