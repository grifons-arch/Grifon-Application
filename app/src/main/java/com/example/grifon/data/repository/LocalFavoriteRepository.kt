package com.example.grifon.data.repository

import com.example.grifon.core.ShopConfig
import com.example.grifon.data.local.FavoriteDao
import com.example.grifon.data.local.FavoriteEntity
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.FavoriteProduct
import com.example.grifon.domain.model.Product
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class LocalFavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val shopPreferences: ShopPreferences,
) : FavoriteRepository {

    override fun observeFavorites(shopId: String): Flow<List<FavoriteProduct>> {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        return shopPreferences.currentCustomerId.flatMapLatest { customerId ->
            if (customerId == null) {
                flowOf(emptyList())
            } else {
                favoriteDao.observeFavorites(customerId, normalizedShopId).map { favorites ->
                    favorites.map { it.toDomain() }
                }
            }
        }
    }

    override fun observeIsFavorite(shopId: String, productId: String): Flow<Boolean> {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        return shopPreferences.currentCustomerId.flatMapLatest { customerId ->
            if (customerId == null) {
                flowOf(false)
            } else {
                favoriteDao.observeIsFavorite(customerId, normalizedShopId, productId)
            }
        }
    }

    override suspend fun toggleFavorite(shopId: String, product: Product): Boolean {
        val customerId = shopPreferences.currentCustomerId.first() ?: return false
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        val isFavorite = favoriteDao.observeIsFavorite(customerId, normalizedShopId, product.id).first()

        if (isFavorite) {
            favoriteDao.deleteFavorite(customerId, normalizedShopId, product.id)
            return false
        }

        favoriteDao.insertFavorite(
            FavoriteEntity(
                customerId = customerId,
                productId = product.id,
                shopId = normalizedShopId,
                title = product.title,
                price = product.price,
                currency = product.currency,
                imageUrl = product.imageUrl,
                brand = product.brand,
                addedAt = System.currentTimeMillis(),
            )
        )
        return true
    }
}

private fun FavoriteEntity.toDomain(): FavoriteProduct {
    return FavoriteProduct(
        productId = productId,
        title = title,
        price = price,
        currency = currency,
        imageUrl = imageUrl,
        brand = brand,
        shopId = shopId,
    )
}
