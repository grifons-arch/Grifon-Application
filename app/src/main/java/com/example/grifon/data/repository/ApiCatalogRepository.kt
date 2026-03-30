package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton
import com.example.grifon.BuildConfig
import com.example.grifon.data.catalog.toDomainFacet
import com.example.grifon.data.catalog.toDomainProduct
import com.example.grifon.data.local.LocalPriceAccessService
import com.example.grifon.data.local.ShopPreferences
import kotlinx.coroutines.flow.first

@Singleton
class ApiCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    private val shopPreferences: ShopPreferences,
    private val localPriceAccessService: LocalPriceAccessService,
) : CatalogRepository {

    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    override fun getCategoryTree(shopId: String): Flow<List<Category>> = flow {
        try {
            val id = ShopConfig.normalizeShopId(shopId).toInt()
            val response = catalogApi.getCategories(shopId = id)
            emit(response.items.map { 
                Category(
                    id = it.id.toString(), 
                    name = it.name ?: "",
                    parentId = null,
                    childrenCount = 0
                ) 
            })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getCategoryFilters(shopId: String, categoryId: String): Flow<List<CatalogFacet>> = flow {
        try {
            val sId = ShopConfig.normalizeShopId(shopId).toInt()
            val customerId = shopPreferences.currentCustomerId.first()
            val canDisplayPrices = localPriceAccessService.canDisplayPrices(
                shopId = shopId,
                customerId = customerId,
                canViewPrices = shopPreferences.canViewPrices.first(),
            )
            val requestCustomerId = customerId?.takeIf { canDisplayPrices }
            val response = catalogApi.getCategoryFilters(
                categoryId = categoryId.toInt(),
                shopId = sId,
                customerId = requestCustomerId,
            )
            emit(response.items.map { it.toDomainFacet() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getProductsByCategory(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> = flow {
        try {
            val sId = ShopConfig.normalizeShopId(shopId).toInt()
            val customerId = shopPreferences.currentCustomerId.first()
            val canDisplayPrices = localPriceAccessService.canDisplayPrices(
                shopId = shopId,
                customerId = customerId,
                canViewPrices = shopPreferences.canViewPrices.first(),
            )
            val requestCustomerId = customerId?.takeIf { canDisplayPrices }
            val response = if (categoryId == "2" || categoryId.isBlank()) {
                catalogApi.getProducts(shopId = sId, pageSize = 100, customerId = requestCustomerId)
            } else {
                catalogApi.getCategoryProducts(
                    categoryId = categoryId.toInt(),
                    shopId = sId,
                    customerId = requestCustomerId,
                )
            }
            
            // ΕΦΑΡΜΟΓΗ ΦΙΛΤΡΩΝ ΣΤΗ ΛΙΣΤΑ
            val filteredProducts = response.items
                .map {
                        it.toDomainProduct(
                        gatewayBaseUrl = gatewayBaseUrl,
                        brand = if (sId == 4) "Grifon GR" else "Grifon SE",
                        showPrice = canDisplayPrices,
                    )
                }
                .filter { product ->
                    val matchesPrice = product.price?.let {
                        it >= filters.priceRange.start && it <= filters.priceRange.endInclusive
                    } ?: true
                    val matchesStock = if (filters.inStockOnly) product.inStock else true
                    val matchesBrand = filters.brands.isEmpty() || filters.brands.contains(product.brand)
                    val matchesRating = product.rating >= filters.ratingMin
                    val selectedColors = filters.colors
                    val matchesColor = if (selectedColors.isNotEmpty()) {
                        selectedColors.any { color ->
                            product.title.contains(color, ignoreCase = true) ||
                                product.attributesMap.values.flatten().any { it.contains(color, ignoreCase = true) }
                        }
                    } else true
                    
                    // Φιλτράρισμα βάσει ονόματος για τις κατηγορίες (π.χ. Μινωικά) αν δεν έχουμε attributes
                    val selectedMinoan = filters.attributes["minoan"] ?: emptySet()
                    val matchesMinoan = if (selectedMinoan.isNotEmpty()) {
                        selectedMinoan.any { product.title.contains(it, ignoreCase = true) }
                    } else true
                    val genericAttributeFilters = filters.attributes.filterKeys { it != "minoan" }
                    val matchesAttributes = genericAttributeFilters.all { (key, values) ->
                        if (values.isEmpty()) {
                            true
                        } else {
                            val productValues = product.attributesMap.entries.firstOrNull {
                                it.key.equals(key, ignoreCase = true)
                            }?.value.orEmpty()
                            productValues.any { values.contains(it) }
                        }
                    }

                    matchesPrice &&
                        matchesStock &&
                        matchesBrand &&
                        matchesRating &&
                        matchesColor &&
                        matchesMinoan &&
                        matchesAttributes
                }
                .let { list ->
                    // ΕΦΑΡΜΟΓΗ ΤΑΞΙΝΟΜΗΣΗΣ
                    when (sortOption) {
                        SortOption.PRICE_LOW_HIGH -> list.sortedBy { it.price ?: Double.MAX_VALUE }
                        SortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price ?: Double.MIN_VALUE }
                        else -> list
                    }
                    Log.d("CrashLog", "Sync: ATOMIC STORE SUCCESSFUL")
                }
            } catch (e: Exception) {
                Log.e("CrashLog", "Sync: FATAL ERROR", e)
            }
        }
    }

    override fun searchProducts(
        shopId: String,
        query: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> = flow {
        try {
            val sId = ShopConfig.normalizeShopId(shopId).toInt()
            val customerId = shopPreferences.currentCustomerId.first()
            val canDisplayPrices = localPriceAccessService.canDisplayPrices(
                shopId = shopId,
                customerId = customerId,
                canViewPrices = shopPreferences.canViewPrices.first(),
            )
            val response = catalogApi.getProducts(
                shopId = sId,
                pageSize = 100,
                customerId = customerId?.takeIf { canDisplayPrices },
            )
            val allProducts = response.items.map {
                it.toDomainProduct(
                    gatewayBaseUrl = gatewayBaseUrl,
                    brand = if (sId == 4) "Grifon GR" else "Grifon SE",
                    showPrice = canDisplayPrices,
                )
            }
            
            val filtered = allProducts.filter { product ->
                val matchesQuery = product.title.contains(query, ignoreCase = true) || 
                                 product.attributesMap["reference"].orEmpty().any { it.contains(query, ignoreCase = true) }
                
                val matchesPrice = product.price?.let {
                    it >= filters.priceRange.start && it <= filters.priceRange.endInclusive
                } ?: true
                val matchesStock = if (filters.inStockOnly) product.inStock else true
                val matchesBrand = filters.brands.isEmpty() || filters.brands.contains(product.brand)
                val matchesRating = product.rating >= filters.ratingMin
                val selectedColors = filters.colors
                val matchesColor = if (selectedColors.isNotEmpty()) {
                    selectedColors.any { color ->
                        product.title.contains(color, ignoreCase = true) ||
                            product.attributesMap.values.flatten().any { it.contains(color, ignoreCase = true) }
                    }
                } else true
                val matchesAttributes = filters.attributes.all { (key, values) ->
                    if (values.isEmpty()) {
                        true
                    } else {
                        val productValues = product.attributesMap.entries.firstOrNull {
                            it.key.equals(key, ignoreCase = true)
                        }?.value.orEmpty()
                        productValues.any { values.contains(it) }
                    }
                }

                matchesQuery &&
                    matchesPrice &&
                    matchesStock &&
                    matchesBrand &&
                    matchesRating &&
                    matchesColor &&
                    matchesAttributes
            }
        }
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flow {
        try {
            val sId = ShopConfig.normalizeShopId(shopId).toInt()
            val customerId = shopPreferences.currentCustomerId.first()
            val canDisplayPrices = localPriceAccessService.canDisplayPrices(
                shopId = shopId,
                customerId = customerId,
                canViewPrices = shopPreferences.canViewPrices.first(),
            )
            val normalizedProductId = productId.substringAfterLast("_").toIntOrNull()
            if (normalizedProductId == null) {
                emit(null)
                return@flow
            }
            val response = catalogApi.getProduct(
                productId = normalizedProductId,
                shopId = sId,
                customerId = customerId?.takeIf { canDisplayPrices },
            )
            emit(
                response.toDomainProduct(
                    gatewayBaseUrl = gatewayBaseUrl,
                    brand = if (sId == 4) "Grifon GR" else "Grifon SE",
                    showPrice = canDisplayPrices,
                )
            )
        } catch (e: Exception) {
            emit(null)
        }
    }

    private fun ProductEntity.toDomain() = Product(id, title, price, currency, imageUrl, emptyList(), brand, 0.0, inStock, mapOf("reference" to reference), listOfNotNull(categoryId))
}
