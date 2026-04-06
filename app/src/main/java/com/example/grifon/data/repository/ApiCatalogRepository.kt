package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.core.PrestaLanguage
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import com.example.grifon.BuildConfig
import com.example.grifon.data.catalog.toDomainFacet
import com.example.grifon.data.catalog.toDomainProduct
import com.example.grifon.data.local.LocalPriceAccessService
import com.example.grifon.data.local.ShopPreferences
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val DEFAULT_FILTER_PRICE_RANGE = 0.0..500.0

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
            val langId = PrestaLanguage.toLangId(shopPreferences.appLanguage.first())
            val response = catalogApi.getCategories(shopId = id, lang = langId)
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
            val langId = PrestaLanguage.toLangId(shopPreferences.appLanguage.first())
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
                lang = langId,
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
            val langId = PrestaLanguage.toLangId(shopPreferences.appLanguage.first())
            val customerId = shopPreferences.currentCustomerId.first()
            val canDisplayPrices = localPriceAccessService.canDisplayPrices(
                shopId = shopId,
                customerId = customerId,
                canViewPrices = shopPreferences.canViewPrices.first(),
            )
            val requestCustomerId = customerId?.takeIf { canDisplayPrices }
            val apiFilters = filters.toApiBasicFilters(canDisplayPrices)
            val apiSort = sortOption.toApiSort()
            val response = if (categoryId == "2" || categoryId.isBlank()) {
                catalogApi.getProducts(
                    shopId = sId,
                    lang = langId,
                    pageSize = 100,
                    sort = apiSort,
                    priceMin = apiFilters.priceMin,
                    priceMax = apiFilters.priceMax,
                    colors = apiFilters.colors,
                    attributes = apiFilters.attributes,
                    customerId = requestCustomerId,
                )
            } else {
                catalogApi.getCategoryProducts(
                    categoryId = categoryId.toInt(),
                    shopId = sId,
                    lang = langId,
                    sort = apiSort,
                    priceMin = apiFilters.priceMin,
                    priceMax = apiFilters.priceMax,
                    colors = apiFilters.colors,
                    attributes = apiFilters.attributes,
                    customerId = requestCustomerId,
                )
            }

            val products = response.items
                .map {
                    it.toDomainProduct(
                        gatewayBaseUrl = gatewayBaseUrl,
                        brand = if (sId == 4) "Grifon GR" else "Grifon SE",
                        showPrice = canDisplayPrices,
                    )
                }

            emit(products.applyFallbackFilters(filters, sortOption))
        } catch (e: Exception) {
            emit(emptyList())
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
            val langId = PrestaLanguage.toLangId(shopPreferences.appLanguage.first())
            val customerId = shopPreferences.currentCustomerId.first()
            val canDisplayPrices = localPriceAccessService.canDisplayPrices(
                shopId = shopId,
                customerId = customerId,
                canViewPrices = shopPreferences.canViewPrices.first(),
            )
            val apiFilters = filters.toApiBasicFilters(canDisplayPrices, query)
            val requestCustomerId = customerId?.takeIf { canDisplayPrices }
            val searchCandidates = fetchSearchCandidates(
                shopId = sId,
                langId = langId,
                requestCustomerId = requestCustomerId,
                sortOption = sortOption,
                apiFilters = apiFilters,
            )
            val products = searchCandidates.map {
                it.toDomainProduct(
                    gatewayBaseUrl = gatewayBaseUrl,
                    brand = if (sId == 4) "Grifon GR" else "Grifon SE",
                    showPrice = canDisplayPrices,
                )
            }

            emit(
                products
                    .filter { it.matchesSearchQuery(query) }
                    .applyFallbackFilters(filters, sortOption)
            )
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flow {
        try {
            val sId = ShopConfig.normalizeShopId(shopId).toInt()
            val langId = PrestaLanguage.toLangId(shopPreferences.appLanguage.first())
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
                lang = langId,
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

    private suspend fun fetchSearchCandidates(
        shopId: Int,
        langId: Int,
        requestCustomerId: Int?,
        sortOption: SortOption,
        apiFilters: ApiBasicFilters,
    ): List<com.example.grifon.data.catalog.ProductDto> {
        val pageSize = 250
        val maxPages = 20
        val products = LinkedHashMap<Int, com.example.grifon.data.catalog.ProductDto>()

        for (page in 1..maxPages) {
            val response = catalogApi.getProducts(
                shopId = shopId,
                lang = langId,
                page = page,
                pageSize = pageSize,
                sort = sortOption.toApiSort(),
                search = null,
                priceMin = apiFilters.priceMin,
                priceMax = apiFilters.priceMax,
                colors = apiFilters.colors,
                attributes = apiFilters.attributes,
                customerId = requestCustomerId,
            )

            response.items.forEach { item ->
                products[item.id] = item
            }

            if (response.items.size < pageSize) {
                break
            }
        }

        return products.values.toList()
    }
}

private data class ApiBasicFilters(
    val search: String? = null,
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val colors: String? = null,
    val attributes: String? = null,
)

private fun FilterState.hasCustomPriceRange(): Boolean {
    return priceRange.start != DEFAULT_FILTER_PRICE_RANGE.start ||
        priceRange.endInclusive != DEFAULT_FILTER_PRICE_RANGE.endInclusive
}

private fun FilterState.selectedPriceRange(): ClosedFloatingPointRange<Double>? {
    return priceRange.takeIf { hasCustomPriceRange() }
}

private fun FilterState.toApiBasicFilters(
    canDisplayPrices: Boolean,
    search: String? = null,
): ApiBasicFilters {
    val attributeJson = attributes
        .filterValues { values -> values.isNotEmpty() }
        .takeIf { it.isNotEmpty() }
        ?.let { values ->
            JSONObject().apply {
                values.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (key, selectedValues) ->
                    put(key, JSONArray(selectedValues.toList().sorted()))
                }
            }.toString()
        }

    val priceRange = selectedPriceRange().takeIf { canDisplayPrices }
    return ApiBasicFilters(
        search = search?.trim()?.takeIf { it.isNotEmpty() },
        priceMin = priceRange?.start,
        priceMax = priceRange?.endInclusive,
        colors = colors.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(","),
        attributes = attributeJson,
    )
}

private fun SortOption.toApiSort(): String = when (this) {
    SortOption.PRICE_LOW_HIGH -> "[price_ASC]"
    SortOption.PRICE_HIGH_LOW -> "[price_DESC]"
    else -> "[id_DESC]"
}

private fun List<Product>.applyFallbackFilters(
    filters: FilterState,
    sortOption: SortOption,
): List<Product> {
    val filtered = filter { product ->
        val matchesStock = if (filters.inStockOnly) product.inStock else true
        val matchesBrand = filters.brands.isEmpty() || filters.brands.contains(product.brand)
        val matchesRating = product.rating >= filters.ratingMin

        matchesStock && matchesBrand && matchesRating
    }

    return when (sortOption) {
        SortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.price ?: Double.MAX_VALUE }
        SortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price ?: Double.MIN_VALUE }
        else -> filtered
    }
}

private fun Product.matchesSearchQuery(query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) {
        return true
    }

    val referenceValues = attributesMap.entries
        .filter { it.key.equals("reference", ignoreCase = true) }
        .flatMap { it.value }

    return title.contains(needle, ignoreCase = true) ||
        referenceValues.any { it.contains(needle, ignoreCase = true) }
}
