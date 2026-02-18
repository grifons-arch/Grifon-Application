package com.example.grifon.data.fake

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.data.repository.CatalogRepository
import com.example.grifon.data.repository.CartRepository
import com.example.grifon.data.repository.ShopRepository
import com.example.grifon.data.repository.UserRepository
import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.Shop
import com.example.grifon.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeShopRepository(
    private val preferences: ShopPreferences,
    private val catalogApi: CatalogApi,
) : ShopRepository {
    override fun getShops(): Flow<List<Shop>> = flow {
        val remoteShops = runCatching { catalogApi.getShops() }
            .getOrDefault(listOf())

        val mapped = if (remoteShops.isNotEmpty()) {
            remoteShops.map { shop ->
                val shopCode = shop.code?.lowercase() ?: shop.id.toString()
                Shop(id = "shop_$shopCode", name = shop.code ?: "Shop ${shop.id}")
            }
        } else {
            listOf(
                Shop("shop_a", "Shop A"),
                Shop("shop_b", "Shop B"),
            )
        }

        emit(mapped)
    }

    override fun getActiveShopId(): Flow<String> = preferences.activeShopId

    override suspend fun setActiveShopId(shopId: String) {
        preferences.setActiveShopId(shopId)
    }
}

class FakeCatalogRepository : CatalogRepository {
    override fun getCategoryTree(shopId: String): Flow<List<Category>> =
        flowOf(FakeCatalogData.categories)

    override fun getProductsByCategory(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption,
    ): Flow<List<Product>> {
        val base = FakeCatalogData.shopProducts[shopId].orEmpty()
            .filter { product ->
                product.title.contains(categoryId, ignoreCase = true) || categoryId.isBlank()
            }
        return flowOf(applyFiltersAndSort(base, filters, sortOption))
    }

    override fun searchProducts(
        shopId: String,
        query: String,
        filters: FilterState,
        sortOption: SortOption,
    ): Flow<List<Product>> {
        val base = FakeCatalogData.shopProducts[shopId].orEmpty()
            .filter { product ->
                product.title.contains(query, ignoreCase = true) || query.isBlank()
            }
        return flowOf(applyFiltersAndSort(base, filters, sortOption))
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> {
        val product = FakeCatalogData.shopProducts[shopId].orEmpty().find { it.id == productId }
        return flowOf(product)
    }

    private fun applyFiltersAndSort(
        products: List<Product>,
        filters: FilterState,
        sortOption: SortOption,
    ): List<Product> {
        var filtered = products.filter { product ->
            product.price in filters.priceRange &&
                (!filters.inStockOnly || product.inStock) &&
                (filters.ratingMin <= product.rating) &&
                (filters.brands.isEmpty() || filters.brands.contains(product.brand))
        }
        filters.attributes.forEach { (key, values) ->
            if (values.isNotEmpty()) {
                filtered = filtered.filter { product ->
                    values.contains(product.attributesMap[key])
                }
            }
        }
        return when (sortOption) {
            SortOption.RELEVANCE -> filtered
            SortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.price }
            SortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price }
            SortOption.RATING -> filtered.sortedByDescending { it.rating }
        }
    }
}

class FakeCartRepository : CartRepository {
    private val cartState = MutableStateFlow<Map<String, List<CartItem>>>(emptyMap())

    override fun observeCart(shopId: String): Flow<List<CartItem>> =
        cartState.map { it[shopId].orEmpty() }

    override suspend fun addToCart(shopId: String, item: CartItem) {
        updateCart(shopId) { items ->
            val existing = items.find { it.productId == item.productId }
            if (existing == null) items + item else items.map {
                if (it.productId == item.productId) it.copy(qty = it.qty + item.qty) else it
            }
        }
    }

    override suspend fun removeFromCart(shopId: String, productId: String) {
        updateCart(shopId) { items -> items.filterNot { it.productId == productId } }
    }

    override suspend fun updateQuantity(shopId: String, productId: String, qty: Int) {
        updateCart(shopId) { items ->
            if (qty <= 0) items.filterNot { it.productId == productId } else items.map {
                if (it.productId == productId) it.copy(qty = qty) else it
            }
        }
    }

    private fun updateCart(shopId: String, updater: (List<CartItem>) -> List<CartItem>) {
        val current = cartState.value
        val updated = updater(current[shopId].orEmpty())
        cartState.value = current + (shopId to updated)
    }
}

class FakeUserRepository : UserRepository {
    private val loggedIn = MutableStateFlow(false)
    override fun isLoggedIn(): Flow<Boolean> = loggedIn
}
