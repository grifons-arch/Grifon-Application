package com.example.grifon.data.fake

import android.util.Log
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.ShopDto
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.data.repository.CatalogRepository
import com.example.grifon.data.repository.CartRepository
import com.example.grifon.data.repository.ShopRepository
import com.example.grifon.data.repository.UserRepository
import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.model.CatalogFacet
import com.example.grifon.domain.model.CatalogFacetOption
import com.example.grifon.domain.model.CatalogFacetType
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.Shop
import com.example.grifon.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeShopRepository(private val preferences: ShopPreferences, private val catalogApi: CatalogApi) : ShopRepository {
    override fun getShops(): Flow<List<Shop>> = flow {
        val remoteShops = runCatching { catalogApi.getShops() }.getOrElse {
            listOf(
                ShopDto(id = ShopConfig.GreekShopId.toInt()),
                ShopDto(id = ShopConfig.SwedishShopId.toInt()),
            )
        }
        emitAll(
            preferences.appLanguage.map {
                remoteShops.map { shop ->
                    val rawId = shop.code ?: shop.id.toString()
                    val normalizedId = ShopConfig.normalizeShopId(rawId)
                    Shop(
                        id = normalizedId,
                        name = ShopConfig.displayName(rawId),
                    )
                }
            }
        )
    }
    override fun getActiveShopId(): Flow<String> = preferences.activeShopId
    override suspend fun setActiveShopId(shopId: String) = preferences.setActiveShopId(shopId)
}

class FakeCatalogRepository : CatalogRepository {
    override fun getCategoryTree(shopId: String): Flow<List<Category>> =
        flowOf(FakeCatalogData.categories)

    override fun getCategoryFilters(shopId: String, categoryId: String): Flow<List<CatalogFacet>> {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        val base = FakeCatalogData.shopProducts[normalizedShopId].orEmpty()
        val products = base.filter { product ->
            product.title.contains(categoryId, ignoreCase = true) || categoryId.isBlank()
        }
        val attributeCounts = linkedMapOf<String, MutableMap<String, Int>>()
        products.forEach { product ->
            product.attributesMap.forEach { (key, values) ->
                val bucket = attributeCounts.getOrPut(key) { linkedMapOf() }
                values.forEach { value ->
                    bucket[value] = (bucket[value] ?: 0) + 1
                }
            }
        }
        val facets = attributeCounts.map { (key, values) ->
            CatalogFacet(
                key = key,
                title = key,
                type = if (key.equals("Color", ignoreCase = true)) CatalogFacetType.COLOR else CatalogFacetType.ATTRIBUTE,
                options = values.map { (value, count) -> CatalogFacetOption(value, value, count) }
            )
        }
        return flowOf(facets)
    }

    override fun getProductsByCategory(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption,
    ): Flow<List<Product>> {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        val base = FakeCatalogData.shopProducts[normalizedShopId].orEmpty()
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
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        val base = FakeCatalogData.shopProducts[normalizedShopId].orEmpty()
            .filter { product ->
                product.title.contains(query, ignoreCase = true) || query.isBlank()
            }
        return flowOf(applyFiltersAndSort(base, filters, sortOption))
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        val product = FakeCatalogData.shopProducts[normalizedShopId].orEmpty().find { it.id == productId }
        return flowOf(product)
    }

    override suspend fun syncCatalog(shopId: String) = Unit

    private fun applyFiltersAndSort(
        products: List<Product>,
        filters: FilterState,
        sortOption: SortOption,
    ): List<Product> {
        var filtered = products.filter { product ->
            (product.price?.let { it in filters.priceRange } ?: true) &&
                (!filters.inStockOnly || product.inStock) &&
                (filters.ratingMin <= product.rating) &&
                (filters.brands.isEmpty() || filters.brands.contains(product.brand)) &&
                (filters.colors.isEmpty() || filters.colors.any { color ->
                    product.title.contains(color, ignoreCase = true) ||
                        product.attributesMap.values.flatten().any { it.contains(color, ignoreCase = true) }
                })
        }
        filters.attributes.forEach { (key, values) ->
            if (values.isNotEmpty()) {
                filtered = filtered.filter { product ->
                    product.attributesMap[key].orEmpty().any { values.contains(it) }
                }
            }
        }
        return when (sortOption) {
            SortOption.RELEVANCE -> filtered
            SortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.price ?: Double.MAX_VALUE }
            SortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price ?: Double.MIN_VALUE }
            SortOption.RATING -> filtered.sortedByDescending { it.rating }
        }
    }
}

class FakeCartRepository : CartRepository {
    private val cartState = MutableStateFlow<Map<String, List<CartItem>>>(emptyMap())

    override fun observeCart(shopId: String): Flow<List<CartItem>> =
        cartState.map { it[ShopConfig.normalizeShopId(shopId)].orEmpty() }

    override suspend fun addToCart(shopId: String, item: CartItem) {
        updateCart(ShopConfig.normalizeShopId(shopId)) { items ->
            val existing = items.find { it.productId == item.productId }
            if (existing == null) items + item else items.map {
                if (it.productId == item.productId) {
                    it.copy(
                        title = item.title,
                        productCode = item.productCode,
                        imageUrl = item.imageUrl,
                        qty = it.qty + item.qty,
                        priceSnapshot = item.priceSnapshot,
                        currency = item.currency,
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun removeFromCart(shopId: String, productId: String) {
        updateCart(ShopConfig.normalizeShopId(shopId)) { items ->
            items.filterNot { it.productId == productId }
        }
    }

    override suspend fun updateQuantity(shopId: String, productId: String, qty: Int) {
        updateCart(ShopConfig.normalizeShopId(shopId)) { items ->
            if (qty <= 0) items.filterNot { it.productId == productId } else items.map {
                if (it.productId == productId) it.copy(qty = qty) else it
            }
        }
    }

    override suspend fun clearCart(shopId: String) {
        updateCart(ShopConfig.normalizeShopId(shopId)) { emptyList() }
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

    override suspend fun login(email: String, pass: String): Result<Unit> {
        loggedIn.value = true
        return Result.success(Unit)
    }

    override fun logout() {
        loggedIn.value = false
    }
}
