package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi
) : CatalogRepository {

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
            // Αν ζητάμε την "αρχική" (συνήθως ID 2), φέρνουμε όλα τα προϊόντα για καλύτερο αποτέλεσμα
            val response = if (categoryId == "2" || categoryId.isBlank()) {
                catalogApi.getProducts(shopId = sId, pageSize = 50)
            } else {
                catalogApi.getCategoryProducts(categoryId = categoryId.toInt(), shopId = sId)
            }
            emit(response.items.map { it.toDomain(sId) })
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
            val filtered = response.items.filter { 
                it.name?.contains(query, ignoreCase = true) == true || 
                it.reference?.contains(query, ignoreCase = true) == true
            }
            emit(filtered.map { it.toDomain(sId) })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flow {
        emit(null)
    }

    private fun com.example.grifon.data.catalog.ProductDto.toDomain(shopId: Int): Product {
        return Product(
            id = "${shopId}_$id",
            title = name ?: "",
            price = price ?: 0.0,
            currency = "EUR",
            imageUrl = defaultImage?.url ?: "",
            brand = if (shopId == 4) "Grifon GR" else "Grifon SE",
            rating = 0.0,
            inStock = true,
            attributesMap = mapOf("reference" to (reference ?: ""))
        )
    }
}
