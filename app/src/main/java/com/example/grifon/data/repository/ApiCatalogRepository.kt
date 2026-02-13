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
        val id = shopId.toIntOrNull() ?: 1
        val response = catalogApi.getCategories(shopId = id)
        emit(response.items.map { 
            Category(
                id = it.id.toString(), 
                name = it.name ?: "",
                parentId = null,
                childrenCount = 0
            ) 
        })
    }

    override fun getProductsByCategory(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> = flow {
        val sId = shopId.toIntOrNull() ?: 1
        val cId = categoryId.toIntOrNull() ?: 2
        val response = catalogApi.getCategoryProducts(categoryId = cId, shopId = sId)
        emit(response.items.map { it.toDomain(sId) })
    }

    override fun searchProducts(
        shopId: String,
        query: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> = flow {
        val sId = shopId.toIntOrNull() ?: 1
        val response = catalogApi.getCategoryProducts(categoryId = 2, shopId = sId)
        val filtered = response.items.filter { it.name?.contains(query, ignoreCase = true) == true }
        emit(filtered.map { it.toDomain(sId) })
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
            brand = "Shop $shopId",
            rating = 0.0,
            inStock = true,
            attributesMap = mapOf("reference" to (reference ?: ""))
        )
    }
}
