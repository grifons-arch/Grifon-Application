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
                Log.d("Sync", "STARTING GREEK TRANSLATION SYNC for shop $shopId...")
                val sId = shopId.toIntOrNull() ?: 4
                
                // 1. SYNC CATEGORIES
                val catResponse = try {
                    catalogApi.getCategories(shopId = sId, pageSize = 500)
                } catch (e: Exception) {
                    null
                }

                catResponse?.let { resp ->
                    val entities = resp.items.map { dto ->
                        val originalId = dto.id.toString()
                        CategoryEntity(
                            id = "${shopId}_$originalId",
                            name = translateCategoryName(originalId, dto.name ?: ""),
                            parentId = dto.parentId?.let { p -> "${shopId}_$p" },
                            position = 0,
                            active = true,
                            shopId = shopId
                        )
                    }
                    categoryDao.insertCategories(entities)
                }

                // 2. SYNC PRODUCTS
                val productsMap = mutableMapOf<String, ProductEntity>()
                val refsSet = mutableSetOf<ProductCategoryCrossRef>()
                val limit = 1000

                val resp1 = try { catalogApi.getProducts(shopId = sId, page = 1, pageSize = limit) } catch (e: Exception) { null }
                resp1?.let { collectIntoMaps(it, shopId, productsMap, refsSet) }

                if (resp1?.items?.size ?: 0 >= limit) {
                    val resp2 = try { catalogApi.getProducts(shopId = sId, page = 2, pageSize = limit) } catch (e: Exception) { null }
                    resp2?.let { collectIntoMaps(it, shopId, productsMap, refsSet) }
                }

                if (productsMap.isNotEmpty()) {
                    productDao.clearProductsByShop(shopId)
                    productDao.insertProducts(productsMap.values.toList())
                    categoryDao.insertProductCategoryRefs(refsSet.toList())
                }
                
                Log.d("Sync", "SYNC FINISHED WITH GREEK NAMES.")
            } catch (e: Exception) {
                Log.e("Sync", "Sync failed", e)
            }
        }
    }

    private fun translateCategoryName(id: String, defaultName: String): String {
        return when (id) {
            "2" -> "Αρχική"
            "4000" -> "Κεραμικά"
            "4025" -> "Διακοσμητικά Κεραμικά"
            "4030" -> "Φανάρια, Καντήλια"
            "4045" -> "Μινωικά"
            "4500" -> "Αγαλματίδια κ.α."
            "4504" -> "Βερονέζ"
            "4510" -> "Αλαβάστρινα"
            "4520" -> "Μπρούτζινα"
            "4530" -> "Πολυεστερικά"
            "4550" -> "Γύψινα, Πωρόλιθος, Μαρμάρινα"
            "5000" -> "Διακοσμητικά"
            "5015" -> "Κρεμαστά"
            "5025" -> "Φανάρια, Καντήλια"
            "5030" -> "Επιτραπέζια"
            "5040" -> "Φωτιστικά"
            "5080" -> "Ρολόγια"
            "7000" -> "Χόμπι και παιχνίδια"
            "7025" -> "Τάβλι, Σκάκι"
            "7040" -> "Παιχνίδια, Λούτρινα"
            "7500" -> "Για χρήση"
            "7540" -> "Κουζίνας κ υαλικά"
            "7545" -> "Σαπούνια"
            "8000" -> "Αξεσουάρ"
            "8030" -> "Υφασμάτινα και τσάντες"
            else -> defaultName
        }
    }

    private fun collectIntoMaps(response: ProductsResponseDto, shopId: String, productsMap: MutableMap<String, ProductEntity>, refsSet: MutableSet<ProductCategoryCrossRef>) {
        response.items.forEach { dto ->
            val pId = dto.id.toString()
            val compositeId = "${shopId}_$pId"
            productsMap[compositeId] = ProductEntity(
                id = compositeId,
                title = dto.name ?: "",
                price = dto.price ?: 0.0,
                currency = "EUR",
                imageUrl = if (dto.defaultImage?.url?.startsWith("/") == true) "$gatewayBaseUrl${dto.defaultImage.url}" else dto.defaultImage?.url ?: "",
                brand = "Grifon",
                inStock = true,
                reference = dto.reference ?: "",
                shopId = shopId,
                categoryId = dto.categories?.firstOrNull()?.let { "${shopId}_${it.id}" }
            )
            dto.categories?.forEach { cat ->
                refsSet.add(ProductCategoryCrossRef(compositeId, "${shopId}_${cat.id}"))
            }
        }
    }

    override fun getProductsByCategory(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption
    ): Flow<List<Product>> {
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

    private fun CategoryEntity.toDomain() = Category(id, name, parentId, 0)
    private fun ProductEntity.toDomain() = Product(id, title, price, currency, imageUrl, emptyList(), brand, 0.0, inStock, mapOf("reference" to reference), listOfNotNull(categoryId))
}
