package com.example.grifon.data.repository

import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.FavoriteProduct
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.RecentProduct
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
    suspend fun login(email: String, pass: String): Result<Unit>
    fun logout()
}

interface FavoriteRepository {
    fun observeFavorites(shopId: String): Flow<List<FavoriteProduct>>
    fun observeIsFavorite(shopId: String, productId: String): Flow<Boolean>
    suspend fun toggleFavorite(shopId: String, product: Product): Boolean
}

interface RecentProductRepository {
    fun observeRecentProducts(shopId: String, limit: Int = 10): Flow<List<RecentProduct>>
    suspend fun recordVisit(shopId: String, product: Product)
}

interface WholesaleCustomerRepository {
    suspend fun syncWholesaleCustomers(shopId: String): Int
}

interface CustomerRepository {
    suspend fun syncCustomers(shopId: String): Int
}

interface ProductRepository {
    suspend fun syncProducts(shopId: String): Int
}
