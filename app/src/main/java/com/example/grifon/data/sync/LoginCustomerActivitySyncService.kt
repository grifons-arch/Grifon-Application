package com.example.grifon.data.sync

import androidx.room.withTransaction
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.ProductActivityItemDto
import com.example.grifon.data.catalog.ProductActivityRequestDto
import com.example.grifon.data.catalog.ProductSnapshotDto
import com.example.grifon.data.local.AppDatabase
import com.example.grifon.data.local.FavoriteEntity
import com.example.grifon.data.local.RecentProductEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginCustomerActivitySyncService @Inject constructor(
    private val appDatabase: AppDatabase,
    private val catalogApi: CatalogApi,
) {
    suspend fun syncAfterLogin(customerId: Int, shopId: String) {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        val numericShopId = normalizedShopId.toInt()

        pushLocalFavorites(customerId, normalizedShopId, numericShopId)
        pushLocalRecentProducts(customerId, normalizedShopId, numericShopId)
        pullServerFavorites(customerId, normalizedShopId, numericShopId)
        pullServerRecentProducts(customerId, normalizedShopId, numericShopId)
    }

    private suspend fun pushLocalFavorites(customerId: Int, shopId: String, numericShopId: Int) {
        val favorites = appDatabase.favoriteDao().getFavorites(customerId, shopId)
        favorites.forEach { favorite ->
            val productId = favorite.productId.toIntOrNull() ?: return@forEach
            runCatching {
                catalogApi.syncFavoriteProduct(
                    ProductActivityRequestDto(
                        customerId = customerId,
                        shopId = numericShopId,
                        productId = productId,
                        isFavorite = true,
                        product = favorite.toSnapshotDto(),
                    )
                )
            }
        }
    }

    private suspend fun pushLocalRecentProducts(customerId: Int, shopId: String, numericShopId: Int) {
        val items = appDatabase.recentProductDao().getRecentProducts(customerId, shopId, limit = 20)
        items.forEach { recent ->
            val productId = recent.productId.toIntOrNull() ?: return@forEach
            runCatching {
                catalogApi.syncRecentProduct(
                    ProductActivityRequestDto(
                        customerId = customerId,
                        shopId = numericShopId,
                        productId = productId,
                        product = recent.toSnapshotDto(),
                    )
                )
            }
        }
    }

    private suspend fun pullServerFavorites(customerId: Int, shopId: String, numericShopId: Int) {
        val response = runCatching {
            catalogApi.getFavoriteProducts(customerId = customerId, shopId = numericShopId)
        }.getOrNull() ?: return

        val entities = response.items.map { it.toFavoriteEntity(customerId, shopId) }
        appDatabase.withTransaction {
            appDatabase.favoriteDao().clearFavorites(customerId, shopId)
            if (entities.isNotEmpty()) {
                appDatabase.favoriteDao().insertFavorites(entities)
            }
        }
    }

    private suspend fun pullServerRecentProducts(customerId: Int, shopId: String, numericShopId: Int) {
        val response = runCatching {
            catalogApi.getRecentProducts(customerId = customerId, shopId = numericShopId, limit = 20)
        }.getOrNull() ?: return

        val entities = response.items.map { it.toRecentEntity(customerId, shopId) }
        appDatabase.withTransaction {
            appDatabase.recentProductDao().clearRecentProducts(customerId, shopId)
            if (entities.isNotEmpty()) {
                appDatabase.recentProductDao().upsertRecentProducts(entities)
            }
        }
    }
}

private fun FavoriteEntity.toSnapshotDto(): ProductSnapshotDto {
    return ProductSnapshotDto(
        title = title,
        price = price,
        currency = currency,
        imageUrl = imageUrl,
        brand = brand,
    )
}

private fun RecentProductEntity.toSnapshotDto(): ProductSnapshotDto {
    return ProductSnapshotDto(
        title = title,
        price = price,
        currency = currency,
        imageUrl = imageUrl,
        brand = brand,
    )
}

private fun ProductActivityItemDto.toFavoriteEntity(customerId: Int, shopId: String): FavoriteEntity {
    return FavoriteEntity(
        customerId = customerId,
        productId = productId.toString(),
        shopId = shopId,
        title = title.orEmpty(),
        price = price,
        currency = currency.orEmpty(),
        imageUrl = imageUrl.orEmpty(),
        brand = brand.orEmpty(),
        addedAt = updatedAt ?: System.currentTimeMillis(),
    )
}

private fun ProductActivityItemDto.toRecentEntity(customerId: Int, shopId: String): RecentProductEntity {
    return RecentProductEntity(
        customerId = customerId,
        productId = productId.toString(),
        shopId = shopId,
        title = title.orEmpty(),
        price = price,
        currency = currency.orEmpty(),
        imageUrl = imageUrl.orEmpty(),
        brand = brand.orEmpty(),
        visitedAt = visitedAt ?: System.currentTimeMillis(),
    )
}
