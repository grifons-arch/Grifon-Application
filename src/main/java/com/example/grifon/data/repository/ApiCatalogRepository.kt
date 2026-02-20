package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.ProductsResponseDto
import com.example.grifon.data.local.*
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
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

    override fun getCategoryTree(shopId: String): Flow<List<Category>> = flow {
        // 1. Εκπομπή από τοπική βάση
        categoryDao.getCategoriesByShop(shopId).collect { entities ->
            if (entities.isNotEmpty()) {
                Log.d("Catalog", "Emitting ${entities.size} categories from DB")
                emit(entities.map { it.toDomain() })
            } else {
                // 2. Αν είναι άδεια η βάση, κάνε μια άμεση κλήση στο API (Fallback)
                try {
                    Log.d("Catalog", "DB empty, fetching categories from API fallback...")
                    val sId = shopId.toIntOrNull() ?: 4
                    val response = catalogApi.getCategories(shopId = sId, pageSize = 500)
                    val categories = response.items.map { it.toDomain() }
                    emit(categories)
                    
                    // Αποθήκευση στη βάση για την επόμενη φορά
                    categoryDao.insertCategories(response.items.map { 
                        CategoryEntity(it.id.toString(), it.name ?: "", it.parentId?.toString(), 0, true, shopId)
                    })
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }
    }

    override suspend fun syncCatalog(shopId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("Sync", "STARTING SYNC for shop $shopId...")
                val sId = shopId.toIntOrNull() ?: 4
                
                // 1. SYNC CATEGORIES
                val catResponse = catalogApi.getCategories(shopId = sId, pageSize = 500)
                val catEntities = catResponse.items.map { 
                    CategoryEntity(it.id.toString(), it.name ?: "", it.parentId?.toString(), 0, true, shopId)
                }
                categoryDao.insertCategories(catEntities)

                // 2. SYNC PRODUCTS IN BATCHES
                val allProducts = mutableListOf<ProductEntity>()
                val allRefs = mutableListOf<ProductCategoryCrossRef>()
                val limit = 1000

                val response1 = catalogApi.getProducts(shopId = sId, page = 1, pageSize = limit)
                processBatch(response1, shopId, allProducts, allRefs)

                if (response1.items.size >= limit) {
                    val response2 = catalogApi.getProducts(shopId = sId, page = 2, pageSize = limit)
                    processBatch(response2, shopId, allProducts, allRefs)
                }

                if (allProducts.isNotEmpty()) {
                    productDao.clearProductsByShop(shopId)
                    productDao.insertProducts(allProducts)
                    categoryDao.insertProductCategoryRefs(allRefs)
                    Log.d("Sync", "SUCCESS: Stored ${allProducts.size} products and ${allRefs.size} links.")
                }
                
            } catch (e: Exception) {
                Log.e("Sync", "CRITICAL SYNC ERROR", e)
            }
        }
    }

    private fun processBatch(
        response: ProductsResponseDto, 
        shopId: String, 
        products: MutableList<ProductEntity>, 
        refs: MutableList<ProductCategoryCrossRef>
    ) {
        response.items.forEach { dto ->
            products.add(dto.toLocal(shopId))
            dto.categories?.forEach { cat ->
                refs.add(ProductCategoryCrossRef(productId = dto.id.toString(), categoryId = cat.id.toString()))
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
    
    private fun com.example.grifon.data.catalog.CategoryDto.toDomain() = Category(
        id = id.toString(),
        name = name ?: "",
        parentId = parentId?.toString(),
        childrenCount = 0
    )

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
