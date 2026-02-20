package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.local.*
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.grifon.BuildConfig

@Singleton
class ApiCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    private val categoryDao: CategoryDao
) : CatalogRepository {

    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    override fun getCategoryTree(shopId: String): Flow<List<Category>> = flow {
        // Προσπάθεια λήψης από την τοπική βάση πρώτα
        categoryDao.getCategoriesByShop(shopId).collect { entities ->
            if (entities.isNotEmpty()) {
                emit(entities.map { 
                    Category(
                        id = it.id, 
                        name = it.name,
                        parentId = it.parentId,
                        childrenCount = 0
                    ) 
                })
            } else {
                // Αν είναι άδεια, φέρε από το API
                try {
                    val sId = shopId.toIntOrNull() ?: 4
                    val response = catalogApi.getCategories(shopId = sId)
                    val categories = response.items.map { 
                        Category(
                            id = it.id.toString(), 
                            name = it.name ?: "",
                            parentId = it.parentId?.toString(),
                            childrenCount = 0
                        ) 
                    }
                    emit(categories)
                    // Αποθήκευση τοπικά
                    categoryDao.insertCategories(categories.map { 
                        CategoryEntity(
                            id = it.id,
                            name = it.name,
                            parentId = it.parentId,
                            position = 0,
                            active = true,
                            shopId = shopId
                        )
                    })
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }
    }

    override suspend fun syncCatalog(shopId: String) {
        try {
            Log.d("Sync", "Starting catalog sync for shop $shopId")
            val sId = shopId.toIntOrNull() ?: 4
            
            // 1. Συγχρονισμός Κατηγοριών
            val response = catalogApi.getCategories(shopId = sId, pageSize = 500)
            val entities = response.items.map { 
                CategoryEntity(
                    id = it.id.toString(),
                    name = it.name ?: "",
                    parentId = it.parentId?.toString(),
                    position = 0,
                    active = true,
                    shopId = shopId
                )
            }
            categoryDao.insertCategories(entities)
            
            // 2. Συγχρονισμός Υποκατηγοριών (για όσες έχουν parentId != null)
            val subEntities = entities.filter { it.parentId != null }.map { 
                SubCategoryEntity(
                    id = it.id,
                    parentId = it.parentId!!,
                    name = it.name,
                    position = it.position,
                    active = it.active,
                    shopId = it.shopId
                )
            }
            categoryDao.insertSubCategories(subEntities)
            
            Log.d("Sync", "Sync completed: ${entities.size} categories stored.")
        } catch (e: Exception) {
            Log.e("Sync", "Sync failed", e)
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
            
            val filteredProducts = response.items.map { it.toDomain(sId) }
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
            val filtered = response.items.map { it.toDomain(sId) }.filter { 
                it.title.contains(query, ignoreCase = true) 
            }
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
            brand = "Grifon",
            rating = 0.0,
            inStock = true,
            attributesMap = mapOf("reference" to (reference ?: "")),
            categoryIds = categories?.map { it.id.toString() } ?: emptyList()
        )
    }
}
