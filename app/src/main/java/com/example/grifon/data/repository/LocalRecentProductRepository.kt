package com.example.grifon.data.repository

import com.example.grifon.core.ShopConfig
import com.example.grifon.data.local.RecentProductDao
import com.example.grifon.data.local.RecentProductEntity
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.RecentProduct
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class LocalRecentProductRepository @Inject constructor(
    private val recentProductDao: RecentProductDao,
    private val shopPreferences: ShopPreferences,
) : RecentProductRepository {

    override fun observeRecentProducts(shopId: String, limit: Int): Flow<List<RecentProduct>> {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        return shopPreferences.currentCustomerId.flatMapLatest { customerId ->
            if (customerId == null) {
                flowOf(emptyList())
            } else {
                recentProductDao.observeRecentProducts(customerId, normalizedShopId, limit).map { items ->
                    items.map { it.toDomain() }
                }
            }
        }
    }

    override suspend fun recordVisit(shopId: String, product: Product) {
        val customerId = shopPreferences.currentCustomerId.first() ?: return
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        recentProductDao.upsertRecentProduct(
            RecentProductEntity(
                customerId = customerId,
                productId = product.id,
                shopId = normalizedShopId,
                title = product.title,
                price = product.price,
                currency = product.currency,
                imageUrl = product.imageUrl,
                brand = product.brand,
                visitedAt = System.currentTimeMillis(),
            )
        )
        recentProductDao.trimRecentProducts(customerId, normalizedShopId, keep = 20)
    }
}

private fun RecentProductEntity.toDomain(): RecentProduct {
    return RecentProduct(
        productId = productId,
        title = title,
        price = price,
        currency = currency,
        imageUrl = imageUrl,
        brand = brand,
        shopId = shopId,
    )
}
