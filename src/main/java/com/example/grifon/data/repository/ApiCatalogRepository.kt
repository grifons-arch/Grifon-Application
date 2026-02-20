package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.local.*
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.grifon.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ApiCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao
) : CatalogRepository {

    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    override fun getCategoryTree(shopId: String): Flow<List<Category>> = 
        categoryDao.getCategoriesByShop(shopId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun syncCatalog(shopId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("Sync", "STARTING FULL MULTI-BATCH SYNC for shop $shopId...")
                val sId = shopId.toIntOrNull() ?: 4
                
                // 1. SYNC CATEGORIES
                val catResponse = catalogApi.getCategories(shopId = sId, pageSize = 500)
                val catEntities = catResponse.items.map { 
                    CategoryEntity(
                        id = it.id.toString(),
                        name = it.name ?: "",
                        parentId = it.parentId?.toString(),
                        position = 0,
                        active = true,
                        shopId = shopId
                    )
                }
                categoryDao.insertCategories(catEntities)
                
                val subEntities = catEntities.filter { it.parentId != null }.map { 
                    SubCategoryEntity(
                        id = it.id,
                        parentId = it.parentId!!,
                        name = it.name,
                        position = 0,
                        active = true,
                        shopId = shopId
                    )
                }
                if (subEntities.isNotEmpty()) {
                    categoryDao.insertSubCategories(subEntities)
                }
                Log.d("Sync", "Saved ${catEntities.size} cats and ${subEntities.size} subs.")

                // 2. SYNC PRODUCTS (Multi-page fetch)
                val allProductEntities = mutableListOf<ProductEntity>()
                val pageSize = 1000
                
                // Fetch Page 1
                Log.d("Sync", "Fetching products batch 1...")
                val response1 = catalogApi.getProducts(shopId = sId, page = 1, pageSize = pageSize)
                allProductEntities.addAll(response1.items.map { it.toLocal(shopId) })
                
                // Fetch Page 2
                if (response1.items.size >= pageSize) {
                    Log.d("Sync", "Fetching products batch 2...")
                    val response2 = catalogApi.getProducts(shopId = sId, page = 2, pageSize = pageSize)
                    allProductEntities.addAll(response2.items.map { it.toLocal(shopId) })
                }

                if (allProductEntities.isNotEmpty()) {
                    productDao.clearProductsByShop(shopId)
                    productDao.insertProducts(allProductEntities)
                    Log.d("Sync", "SUCCESS: Stored total ${allProductEntities.size} products in Room.")
                }
                
                Log.d("Sync", "FULL SYNC COMPLETED.")
            } catch (e: Exception) {
                Log.e("Sync", "CRITICAL SYNC ERROR", e)
            }
        }
    }

    override fun getProductsByCategory(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> = flow {
        productDao.getProductsByCategory(categoryId, shopId).collect { entities ->
            if (entities.isNotEmpty()) {
                emit(entities.map { it.toDomain() })
            } else {
                try {
                    val sId = shopId.toIntOrNull() ?: 4
                    val response = catalogApi.getCategoryProducts(categoryId = categoryId.toInt(), shopId = sId, pageSize = 500)
                    emit(response.items.map { it.toDomain(sId) })
                } catch (e: Exception) {
                    emit(emptyList())
                }
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
            val sId = shopId.toIntOrNull() ?: 4
            val response = catalogApi.getProducts(shopId = sId, pageSize = 200)
            val filtered = response.items.map { it.toDomain(sId) }.filter { 
                it.title.contains(query, ignoreCase = true) 
            }
            emit(filtered)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flow {
        val local = productDao.getProductById(productId)
        emit(local?.toDomain())
    }

    private fun CategoryEntity.toDomain() = Category(id, name, parentId, 0)
    
    private fun ProductEntity.toDomain() = Product(
        id = id, title = title, price = price, currency = currency,
        imageUrl = imageUrl, brand = brand, rating = 0.0, inStock = inStock,
        attributesMap = mapOf("reference" to reference),
        categoryIds = listOfNotNull(categoryId)
    )

    private fun com.example.grifon.data.catalog.ProductDto.toLocal(shopId: String) = ProductEntity(
        id = id.toString(),
        title = name ?: "",
        price = price ?: 0.0,
        currency = "EUR",
        imageUrl = if (defaultImage?.url?.startsWith("/") == true) "$gatewayBaseUrl${defaultImage.url}" else defaultImage?.url ?: "",
        brand = "Grifon",
        inStock = true,
        reference = reference ?: "",
        shopId = shopId,
        categoryId = categories?.firstOrNull()?.id?.toString()
    )

    private fun com.example.grifon.data.catalog.ProductDto.toDomain(shopId: Int): Product {
        val rawUrl = defaultImage?.url ?: ""
        val fullImageUrl = if (rawUrl.startsWith("/") == true) "$gatewayBaseUrl$rawUrl" else rawUrl
        return Product(
            id = id.toString(),
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
