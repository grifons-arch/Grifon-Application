package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import com.example.grifon.BuildConfig
import com.example.grifon.data.catalog.toDomainProduct

@Singleton
class ApiCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi
) : CatalogRepository {

    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    override fun getCategoryTree(shopId: String): Flow<List<Category>> = flow {
        try {
            val id = shopId.toIntOrNull() ?: 4
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

    override fun getProductsByCategory(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> = flow {
        try {
            val sId = shopId.toIntOrNull() ?: 4
            val response = if (categoryId == "2" || categoryId.isBlank()) {
                catalogApi.getProducts(shopId = sId, pageSize = 100)
            } else {
                catalogApi.getCategoryProducts(categoryId = categoryId.toInt(), shopId = sId)
            }
            
            // ΕΦΑΡΜΟΓΗ ΦΙΛΤΡΩΝ ΣΤΗ ΛΙΣΤΑ
            val filteredProducts = response.items
                .map {
                    it.toDomainProduct(
                        gatewayBaseUrl = gatewayBaseUrl,
                        brand = if (sId == 4) "Grifon GR" else "Grifon SE",
                    )
                }
                .filter { product ->
                    val matchesPrice = product.price >= filters.priceRange.start && product.price <= filters.priceRange.endInclusive
                    val matchesStock = if (filters.inStockOnly) product.inStock else true
                    val matchesBrand = filters.brands.isEmpty() || filters.brands.contains(product.brand)
                    val matchesRating = product.rating >= filters.ratingMin
                    val selectedColors = filters.colors
                    val matchesColor = if (selectedColors.isNotEmpty()) {
                        selectedColors.any { color ->
                            product.title.contains(color, ignoreCase = true) ||
                                product.attributesMap.values.any { it.contains(color, ignoreCase = true) }
                        }
                    } else true
                    
                    // Φιλτράρισμα βάσει ονόματος για τις κατηγορίες (π.χ. Μινωικά) αν δεν έχουμε attributes
                    val selectedMinoan = filters.attributes["minoan"] ?: emptySet()
                    val matchesMinoan = if (selectedMinoan.isNotEmpty()) {
                        selectedMinoan.any { product.title.contains(it, ignoreCase = true) }
                    } else true

                    matchesPrice && matchesStock && matchesBrand && matchesRating && matchesColor && matchesMinoan
                }
                .let { list ->
                    // ΕΦΑΡΜΟΓΗ ΤΑΞΙΝΟΜΗΣΗΣ
                    when (sortOption) {
                        SortOption.PRICE_LOW_HIGH -> list.sortedBy { it.price }
                        SortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
                        else -> list
                    }
                }

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
            val response = catalogApi.getProducts(shopId = sId, pageSize = 100)
            val allProducts = response.items.map {
                it.toDomainProduct(
                    gatewayBaseUrl = gatewayBaseUrl,
                    brand = if (sId == 4) "Grifon GR" else "Grifon SE",
                )
            }
            
            val filtered = allProducts.filter { product ->
                val matchesQuery = product.title.contains(query, ignoreCase = true) || 
                                 product.attributesMap["reference"]?.contains(query, ignoreCase = true) == true
                
                val matchesPrice = product.price >= filters.priceRange.start && product.price <= filters.priceRange.endInclusive
                val matchesStock = if (filters.inStockOnly) product.inStock else true
                val matchesBrand = filters.brands.isEmpty() || filters.brands.contains(product.brand)
                val matchesRating = product.rating >= filters.ratingMin
                val selectedColors = filters.colors
                val matchesColor = if (selectedColors.isNotEmpty()) {
                    selectedColors.any { color ->
                        product.title.contains(color, ignoreCase = true) ||
                            product.attributesMap.values.any { it.contains(color, ignoreCase = true) }
                    }
                } else true
                
                matchesQuery && matchesPrice && matchesStock && matchesBrand && matchesRating && matchesColor
            }
            emit(filtered)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flow {
        try {
            val sId = shopId.toIntOrNull() ?: 4
            val normalizedProductId = productId.substringAfterLast("_").toIntOrNull()
            if (normalizedProductId == null) {
                emit(null)
                return@flow
            }

            val response = catalogApi.getProduct(productId = normalizedProductId, shopId = sId)
            emit(
                response.toDomainProduct(
                    gatewayBaseUrl = gatewayBaseUrl,
                    brand = if (sId == 4) "Grifon GR" else "Grifon SE",
                )
            )
        } catch (e: Exception) {
            emit(null)
        }
    }
}
