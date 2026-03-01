package com.example.grifon.data.repository

import com.example.grifon.BuildConfig
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi
) : CatalogRepository {

    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

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
            val response = if (categoryId == "2" || categoryId.isBlank()) {
                catalogApi.getProducts(
                    shopId = sId,
                    pageSize = 100,
                    minPrice = filters.priceRange.start,
                    maxPrice = filters.priceRange.endInclusive,
                    inStockOnly = filters.inStockOnly
                )
            } else {
                catalogApi.getCategoryProducts(
                    categoryId = categoryId.toInt(),
                    shopId = sId,
                    minPrice = filters.priceRange.start,
                    maxPrice = filters.priceRange.endInclusive,
                    inStockOnly = filters.inStockOnly
                )
            }

            val filteredProducts = response.items
                .map { it.toDomain(sId) }
                .applyClientSideFallbackFilters(filters)
                .applySort(sortOption)

            emit(filteredProducts)
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
            val sId = shopId.toIntOrNull() ?: 4
            val response = catalogApi.getProducts(
                shopId = sId,
                pageSize = 100,
                search = query.takeIf { it.isNotBlank() },
                minPrice = filters.priceRange.start,
                maxPrice = filters.priceRange.endInclusive,
                inStockOnly = filters.inStockOnly
            )

            val filtered = response.items
                .map { it.toDomain(sId) }
                .applyClientSideFallbackFilters(filters)
                .applySort(sortOption)

            emit(filtered)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flow {
        emit(null)
    }

    private fun com.example.grifon.data.catalog.ProductDto.toDomain(shopId: Int): Product {
        val rawUrl = defaultImage?.url ?: ""
        val fullImageUrl = if (rawUrl.startsWith("/")) "$gatewayBaseUrl$rawUrl" else rawUrl

        return Product(
            id = "${shopId}_$id",
            title = name ?: "",
            price = price ?: 0.0,
            currency = "EUR",
            imageUrl = fullImageUrl,
            brand = brand ?: if (shopId == 4) "Grifon GR" else "Grifon SE",
            rating = 0.0,
            inStock = inStock ?: ((quantity ?: 1) > 0),
            attributesMap = mapOf("reference" to (reference ?: ""))
        )
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
}
