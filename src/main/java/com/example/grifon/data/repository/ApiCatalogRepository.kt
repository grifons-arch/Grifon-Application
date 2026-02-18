package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.ProductDto
import com.example.grifon.data.local.*
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.grifon.BuildConfig

@Singleton
class ApiCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao
) : CatalogRepository {

    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    override fun getCategoryTree(shopId: String): Flow<List<Category>> = flow {
        // 1. Πάντα δείχνουμε πρώτα ό,τι έχουμε στη Room
        val localCategories = categoryDao.getCategoriesByShop(shopId).first()
        if (localCategories.isNotEmpty()) {
            emit(localCategories.map { it.toDomain() })
        }

        // 2. Φέρνουμε φρέσκα δεδομένα και ενημερώνουμε τη Room
        try {
            val response = catalogApi.getCategories(shopId = shopId.toInt())
            if (response.items.isNotEmpty()) {
                val entities = response.items.map { dto ->
                    CategoryEntity(
                        id = dto.id.toString(),
                        name = dto.name ?: "",
                        parentId = null,
                        position = 0,
                        active = true,
                        shopId = shopId
                    )
                }
                categoryDao.insertCategories(entities)
                emit(entities.map { it.toDomain() })
            }
        } catch (e: Exception) {
            Log.e("ApiCatalogRepo", "Error fetching categories: ${e.message}")
        }
    }

    override fun getProductsByCategory(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> = flow {
        // 1. Έλεγχος στη Room
        val localProducts = productDao.getProductsByCategory(categoryId, shopId).first()
        if (localProducts.isNotEmpty()) {
            emit(localProducts.map { it.toDomain() })
        }

        // 2. Φέρνουμε από το API
        try {
            // Διόρθωση ID: Αν το ID είναι 4000.1, παίρνουμε το 4000
            val cleanId = categoryId.split(".").first().toIntOrNull() ?: 2
            
            val response = if (cleanId == 2) {
                catalogApi.getProducts(shopId = shopId.toInt(), pageSize = 100)
            } else {
                catalogApi.getCategoryProducts(categoryId = cleanId, shopId = shopId.toInt())
            }
            
            if (response.items.isNotEmpty()) {
                val entities = response.items.map { it.toEntity(shopId, categoryId) }
                productDao.insertProducts(entities)
                emit(entities.map { it.toDomain() })
            } else if (localProducts.isEmpty()) {
                emit(emptyList<Product>())
            }
        } catch (e: Exception) {
            Log.e("ApiCatalogRepo", "Error fetching products for $categoryId: ${e.message}")
            if (localProducts.isEmpty()) emit(emptyList<Product>())
        }
    }

    override fun searchProducts(
        shopId: String,
        query: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> = flow {
        try {
            val response = catalogApi.getProducts(shopId = shopId.toInt(), pageSize = 100)
            val filtered = response.items
                .filter { it.name?.contains(query, ignoreCase = true) == true || 
                         it.reference?.contains(query, ignoreCase = true) == true }
                .map { it.toEntity(shopId, null).toDomain() }
            emit(filtered)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flow {
        val local = productDao.getProductById(productId)
        if (local != null) {
            emit(local.toDomain())
        } else {
            emit(null)
        }
    }

    // MAPPERS
    private fun CategoryEntity.toDomain() = Category(id, name, parentId, 0)
    
    private fun ProductEntity.toDomain() = Product(
        id = id, title = title, price = price, currency = currency,
        imageUrl = imageUrl, brand = brand, rating = 0.0, inStock = inStock,
        attributesMap = mapOf("reference" to reference)
    )

    private fun ProductDto.toEntity(shopId: String, categoryId: String?) = ProductEntity(
        id = "${shopId}_$id",
        title = name ?: "",
        price = price ?: 0.0,
        currency = "EUR",
        imageUrl = if (defaultImage?.url?.startsWith("/") == true) "$gatewayBaseUrl${defaultImage.url}" else defaultImage?.url ?: "",
        brand = "Grifon",
        inStock = true,
        reference = reference ?: "",
        shopId = shopId,
        categoryId = categoryId
    )
}
