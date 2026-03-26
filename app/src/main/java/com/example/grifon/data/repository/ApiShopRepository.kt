package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.ShopDto
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.Shop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiShopRepository @Inject constructor(
    private val preferences: ShopPreferences,
    private val catalogApi: CatalogApi
) : ShopRepository {
    override fun getShops(): Flow<List<Shop>> = flow {
        val shopDtos = runCatching { catalogApi.getShops() }.getOrElse {
            listOf(
                ShopDto(id = ShopConfig.GreekShopId.toInt()),
                ShopDto(id = ShopConfig.SwedishShopId.toInt()),
            )
        }

        emitAll(
            preferences.appLanguage.map {
                shopDtos.map { shop ->
                    val rawId = shop.code ?: shop.id.toString()
                    val normalizedId = ShopConfig.normalizeShopId(rawId)
                    Shop(
                        id = normalizedId,
                        name = ShopConfig.displayName(rawId),
                    )
                }
            }
        )
    }

    override fun getActiveShopId(): Flow<String> = preferences.activeShopId

    override suspend fun setActiveShopId(shopId: String) {
        preferences.setActiveShopId(shopId)
    }
}
