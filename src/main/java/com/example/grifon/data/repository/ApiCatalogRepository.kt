package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.ProductsResponseDto
import com.example.grifon.data.local.*
import com.example.grifon.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
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
        categoryDao.getCategoriesByShop(shopId)
            .distinctUntilChanged()
            .map { entities ->
                entities.map { Category(it.id, it.name, it.parentId, 0) }
            }

    override suspend fun syncCatalog(shopId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("CrashLog", "Sync: STARTING for shop $shopId")
                val sId = shopId.toIntOrNull() ?: 4
                
                // 1. Fetch Categories
                val catResponse = try { catalogApi.getCategories(shopId = sId, pageSize = 500) } catch (e: Exception) { null }
                val catEntities = catResponse?.items?.map { 
                    CategoryEntity("${shopId}_${it.id}", it.name ?: "", it.parentId?.let { p -> "${shopId}_$p" }, 0, true, shopId)
                } ?: emptyList()

                // 2. Fetch Products (Batch 1 & 2)
                val productsMap = mutableMapOf<String, ProductEntity>()
                val refsSet = mutableSetOf<ProductCategoryCrossRef>()
                val limit = 1000

                val resp1 = try { catalogApi.getProducts(shopId = sId, page = 1, pageSize = limit) } catch (e: Exception) { null }
                resp1?.let { collectIntoMaps(it, shopId, productsMap, refsSet) }

                if (resp1?.items?.size ?: 0 >= limit) {
                    val resp2 = try { catalogApi.getProducts(shopId = sId, page = 2, pageSize = limit) } catch (e: Exception) { null }
                    resp2?.let { collectIntoMaps(it, shopId, productsMap, refsSet) }
                }

                // 3. DATABASE WRITE
                if (catEntities.isNotEmpty() || productsMap.isNotEmpty()) {
                    if (catEntities.isNotEmpty()) categoryDao.insertCategories(catEntities)
                    if (productsMap.isNotEmpty()) {
                        productDao.clearProductsByShop(shopId)
                        productDao.insertProducts(productsMap.values.toList())
                        categoryDao.insertProductCategoryRefs(refsSet.toList())
                    }
                    Log.d("CrashLog", "Sync: SUCCESSFUL STORE")
                }
            } catch (e: Exception) {
                Log.e("CrashLog", "Sync: ERROR", e)
            }
        }
    }

    private fun collectIntoMaps(response: ProductsResponseDto, shopId: String, productsMap: MutableMap<String, ProductEntity>, refsSet: MutableSet<ProductCategoryCrossRef>) {
        response.items.forEach { dto ->
            val pId = dto.id.toString()
            val compositeId = "${shopId}_$pId"
            productsMap[compositeId] = ProductEntity(compositeId, dto.name ?: "", dto.price ?: 0.0, "EUR", if (dto.defaultImage?.url?.startsWith("/") == true) "$gatewayBaseUrl${dto.defaultImage.url}" else dto.defaultImage?.url ?: "", "Grifon", true, dto.reference ?: "", shopId, dto.categories?.firstOrNull()?.let { "${shopId}_${it.id}" })
            dto.categories?.forEach { cat ->
                refsSet.add(ProductCategoryCrossRef(compositeId, "${shopId}_${cat.id}"))
            }
        }
    }

    override fun getProductsByCategory(shopId: String, categoryId: String, filters: FilterState, sortOption: SortOption): Flow<List<Product>> {
        return if (categoryId.isBlank() || categoryId.endsWith("_2")) {
            productDao.getProductsByShop(shopId).map { list -> list.map { it.toDomain() } }
        } else {
            categoryDao.getCategoryWithProducts(categoryId).map { list -> list.map { it.toDomain() } }
        }
    }

    override fun searchProducts(shopId: String, query: String, filters: FilterState, sortOption: SortOption): Flow<List<Product>> = 
        productDao.getProductsByShop(shopId).map { entities ->
            entities.filter { it.title.contains(query, ignoreCase = true) }.map { it.toDomain() }
        }

    override fun getProductById(shopId: String, productId: String): Flow<Product?> = flow {
        emit(productDao.getProductById(productId)?.toDomain())
    }

    private fun ProductEntity.toDomain() = Product(id, title, price, currency, imageUrl, emptyList(), brand, 0.0, inStock, mapOf("reference" to reference), listOfNotNull(categoryId))
}
