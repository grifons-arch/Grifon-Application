package com.example.grifon.data.fake

import android.util.Log
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.data.repository.*
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.*

class FakeShopRepository(private val preferences: ShopPreferences, private val catalogApi: CatalogApi) : ShopRepository {
    override fun getShops(): Flow<List<Shop>> = flow {
        val remoteShops = runCatching { catalogApi.getShops() }.getOrDefault(listOf())
        val mapped = if (remoteShops.isNotEmpty()) {
            remoteShops.map { Shop("shop_${it.code?.lowercase() ?: it.id}", it.code ?: "Shop ${it.id}") }
        } else {
            listOf(Shop("shop_a", "Shop A"), Shop("shop_b", "Shop B"))
        }
        emit(mapped)
    }
    override fun getActiveShopId(): Flow<String> = preferences.activeShopId
    override suspend fun setActiveShopId(shopId: String) = preferences.setActiveShopId(shopId)
}

class FakeCatalogRepository : CatalogRepository {
    override fun getCategoryTree(shopId: String): Flow<List<Category>> = flowOf(FakeCatalogData.categories)
    override fun getProductsByCategory(shopId: String, categoryId: String, filters: FilterState, sortOption: SortOption): Flow<List<Product>> = flowOf(FakeCatalogData.shopProducts[shopId].orEmpty())
    override fun searchProducts(shopId: String, query: String, filters: FilterState, sortOption: SortOption): Flow<List<Product>> = flowOf(emptyList())
    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flowOf(null)
    override suspend fun syncCatalog(shopId: String) {}
}

class FakeCartRepository : CartRepository {
    private val cartState = MutableStateFlow<Map<String, List<CartItem>>>(emptyMap())
    override fun observeCart(shopId: String): Flow<List<CartItem>> = cartState.map { it[shopId].orEmpty() }
    override suspend fun addToCart(shopId: String, item: CartItem) {}
    override suspend fun removeFromCart(shopId: String, productId: String) {}
    override suspend fun updateQuantity(shopId: String, productId: String, qty: Int) {}
}

class FakeUserRepository : UserRepository {
    private val loggedInFlow = MutableStateFlow(false)
    private val userNameFlow = MutableStateFlow<String?>(null)
    private val userDetailsFlow = MutableStateFlow<User?>(null)

    override fun isLoggedIn(): Flow<Boolean> = loggedInFlow.asStateFlow()
    override fun getUserName(): Flow<String?> = userNameFlow.asStateFlow()
    override fun getUserDetails(): Flow<User?> = userDetailsFlow.asStateFlow()

    override suspend fun login(email: String, pass: String): Boolean {
        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        userNameFlow.value = name
        userDetailsFlow.value = User(
            email = email,
            firstName = name,
            lastName = "FakeUser",
            company = "Grifon Demo",
            vatNumber = "EL123456789",
            newsletter = true
        )
        loggedInFlow.value = true
        return true
    }

    override suspend fun logout() {
        loggedInFlow.value = false
        userNameFlow.value = null
        userDetailsFlow.value = null
    }
}
