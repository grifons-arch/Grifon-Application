package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.Shop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiShopRepository @Inject constructor(
    private val preferences: ShopPreferences,
    private val catalogApi: CatalogApi
) : ShopRepository {

    private val fallbackShops = listOf(
        Shop(id = ShopConfig.GreekShopId, name = ShopConfig.displayName(ShopConfig.GreekShopId)),
        Shop(id = ShopConfig.SwedishShopId, name = ShopConfig.displayName(ShopConfig.SwedishShopId)),
    )

    override fun getShops(): Flow<List<Shop>> = flow {
        try {
            val response = catalogApi.getShops()
            emit(response.map { shop ->
                val normalizedId = ShopConfig.normalizeShopId(shop.id.toString())
                Shop(
                    id = normalizedId,
                    name = shop.code?.let(ShopConfig::displayName) ?: ShopConfig.displayName(normalizedId),
                )
            })
        } catch (e: Exception) {
            emit(fallbackShops)
        }
    }

    override fun getActiveShopId(): Flow<String> = preferences.activeShopId

    override suspend fun setActiveShopId(shopId: String) {
        preferences.setActiveShopId(shopId)
    }
}
