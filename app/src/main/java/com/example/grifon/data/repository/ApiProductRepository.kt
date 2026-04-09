package com.example.grifon.data.repository

import androidx.room.withTransaction
import com.example.grifon.BuildConfig
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.ProductDto
import com.example.grifon.data.local.AppDatabase
import com.example.grifon.data.local.ProductEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiProductRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val catalogApi: CatalogApi,
) : ProductRepository {
    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    override suspend fun syncProducts(shopId: String): Int {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        val numericShopId = normalizedShopId.toInt()
        val syncedAt = System.currentTimeMillis()
        val entities = mutableListOf<ProductEntity>()
        val pageSize = 500
        var page = 1

        while (true) {
            val response = catalogApi.getProducts(
                shopId = numericShopId,
                page = page,
                pageSize = pageSize,
                customerId = null,
            )
            val pageItems = response.items.map {
                it.toEntity(
                    shopId = normalizedShopId,
                    gatewayBaseUrl = gatewayBaseUrl,
                    brand = if (numericShopId == 4) "Grifon GR" else "Grifon SE",
                    syncedAt = syncedAt,
                )
            }
            entities += pageItems

            if (response.items.size < pageSize) {
                break
            }
            page += 1
        }

        appDatabase.withTransaction {
            appDatabase.productDao().clearShop(normalizedShopId)
            if (entities.isNotEmpty()) {
                appDatabase.productDao().upsertAll(entities)
            }
        }

        return entities.size
    }
}

private fun ProductDto.toEntity(
    shopId: String,
    gatewayBaseUrl: String,
    brand: String,
    syncedAt: Long,
): ProductEntity {
    val normalizedBaseUrl = gatewayBaseUrl.removeSuffix("/")
    val rawUrl = defaultImage?.url.orEmpty()
    val imageUrl = when {
        rawUrl.isBlank() -> ""
        rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
        rawUrl.startsWith("/") -> "$normalizedBaseUrl$rawUrl"
        else -> "$normalizedBaseUrl/$rawUrl"
    }

    return ProductEntity(
        id = id.toString(),
        title = name ?: "#$id",
        price = null,
        currency = "EUR",
        imageUrl = imageUrl,
        brand = brand,
        inStock = true,
        reference = reference.orEmpty(),
        shopId = shopId,
        categoryId = null,
        active = true,
        syncedAt = syncedAt,
    )
}
