package com.example.grifon.data.repository

import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.Shop
import com.example.grifon.domain.model.SortOption
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
}

interface CartRepository {
    fun observeCart(shopId: String): Flow<List<CartItem>>
    suspend fun addToCart(shopId: String, item: CartItem)
    suspend fun removeFromCart(shopId: String, productId: String)
    suspend fun updateQuantity(shopId: String, productId: String, qty: Int)
}

interface UserRepository {
    fun isLoggedIn(): Flow<Boolean>
}
