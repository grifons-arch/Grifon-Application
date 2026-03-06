package com.example.grifon.data.repository

import com.example.grifon.BuildConfig
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.local.UserPreferences
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    private val userPreferences: UserPreferences
) : CatalogRepository {

    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    private suspend fun getCustomerIdInt(): Int? {
        return userPreferences.customerId.firstOrNull()?.toIntOrNull()
    }

    override fun getCategoryTree(shopId: String): Flow<List<Category>> = flow {
        try {
            val id = shopId.toIntOrNull() ?: 4
            val response = catalogApi.getCategories(shopId = id)
            emit(
                response.items.map {
                    Category(
                        id = it.id.toString(),
                        name = it.name ?: "",
                        parentId = it.parentId?.toString(),
                        childrenCount = it.childrenCount ?: 0
                    )
                }
            )
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
            val sId = shopId.toIntOrNull() ?: 4
            val catId = categoryId.toIntOrNull()
            if (catId == null || catId <= 0) {
                emit(emptyList<Product>())
                return@flow
            }
            val minPrice = filters.toRequestMinPrice()
            val maxPrice = filters.toRequestMaxPrice()
            val cId = getCustomerIdInt()

            val response = catalogApi.getCategoryProducts(
                categoryId = catId,
                shopId = sId,
                customerId = cId,
                minPrice = minPrice,
                maxPrice = maxPrice,
                inStockOnly = filters.inStockOnly
            )

            val filteredProducts = response.items
                .map { it.toDomain(sId) }
                .applyClientSideFallbackFilters(filters)
                .applySort(sortOption)

            emit(filteredProducts)
        } catch (e: Exception) {
            emit(emptyList<Product>())
        }
    }

    override fun searchProducts(
        shopId: String,
        query: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> = flow {
        try {
            val sId = shopId.toIntOrNull() ?: 4
            val minPrice = filters.toRequestMinPrice()
            val maxPrice = filters.toRequestMaxPrice()
            val cId = getCustomerIdInt()
            
            val response = catalogApi.getProducts(
                shopId = sId,
                customerId = cId,
                pageSize = 100,
                search = query.takeIf { it.isNotBlank() },
                minPrice = minPrice,
                maxPrice = maxPrice,
                inStockOnly = filters.inStockOnly
            )

            val filtered = response.items
                .map { it.toDomain(sId) }
                .applyClientSideFallbackFilters(filters)
                .applySort(sortOption)

            emit(filtered)
        } catch (e: Exception) {
            emit(emptyList<Product>())
        }
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flow {
        try {
            val sId = shopId.toIntOrNull() ?: 4
            val pId = productId.toIntOrNull()
            if (pId == null || pId <= 0) {
                emit(null)
                return@flow
            }
            val cId = getCustomerIdInt()
            val dto = catalogApi.getProductById(productId = pId, shopId = sId, customerId = cId)
            emit(dto.toDomain(sId))
        } catch (e: Exception) {
            emit(null)
        }
    }

    private fun com.example.grifon.data.catalog.ProductDto.toDomain(shopId: Int): Product {
        // Καθαρισμός: resolveImageUrl επιστρέφει "" αν δεν υπάρχει URL
        val resolvedDefaultImage = resolveImageUrl(defaultImage?.url)
        val resolvedImages = images
            .mapNotNull { image -> resolveImageUrl(image.url).takeIf { it.isNotBlank() } }
            .distinct()
        
        val allImages = buildList {
            if (resolvedDefaultImage.isNotBlank()) add(resolvedDefaultImage)
            addAll(resolvedImages.filterNot { it == resolvedDefaultImage })
        }
        
        // Αν allImages είναι άδειο, το imageUrl θα είναι ""
        val primaryImage = allImages.firstOrNull() ?: ""

        return Product(
            id = id.toString(),
            title = name ?: "",
            price = price ?: 0.0,
            currency = "EUR",
            imageUrl = primaryImage, // Δεν βάζουμε πλέον fallback logo εδώ
            images = allImages,
            brand = brand ?: if (shopId == 4) "Grifon GR" else "Grifon SE",
            rating = 0.0,
            inStock = inStock ?: ((quantity ?: 1) > 0),
            attributesMap = mapOf("reference" to (reference ?: ""))
        )
    }

    private fun resolveImageUrl(rawUrl: String?): String {
        val safeUrl = rawUrl.orEmpty()
        if (safeUrl.isBlank()) return ""
        // Αν το Gateway επιστρέφει null ή άδειο URL, επιστρέφουμε ""
        return if (safeUrl.startsWith("/")) "$gatewayBaseUrl$safeUrl" else safeUrl
    }

    private fun List<Product>.applyClientSideFallbackFilters(filters: FilterState): List<Product> {
        var filtered = this

        if (filters.brands.isNotEmpty()) {
            filtered = filtered.filter { filters.brands.contains(it.brand) }
        }

        if (filters.attributes.isNotEmpty()) {
            filters.attributes.forEach { (key, values) ->
                if (values.isNotEmpty()) {
                    filtered = filtered.filter { product ->
                        val attributeValue = product.attributesMap[key]
                        (attributeValue != null && values.contains(attributeValue)) ||
                            values.any { selected -> product.title.contains(selected, ignoreCase = true) }
                    }
                }
            }
        }

        return filtered
    }

    private fun List<Product>.applySort(sortOption: SortOption): List<Product> {
        return when (sortOption) {
            SortOption.RELEVANCE -> this
            SortOption.PRICE_LOW_HIGH -> sortedBy { it.price }
            SortOption.PRICE_HIGH_LOW -> sortedByDescending { it.price }
            SortOption.RATING -> sortedByDescending { it.rating }
        }
    }

    private fun FilterState.toRequestMinPrice(): Double? =
        if (priceRange.start > 0.0) priceRange.start else null

    private fun FilterState.toRequestMaxPrice(): Double? =
        if (priceRange.endInclusive < Double.MAX_VALUE) priceRange.endInclusive else null
}
