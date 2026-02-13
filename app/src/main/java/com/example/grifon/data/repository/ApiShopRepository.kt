package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
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

    override fun getShops(): Flow<List<Shop>> = flow {
        try {
            val response = catalogApi.getShops()
            emit(response.map { Shop(id = it.id.toString(), name = it.code ?: "Shop ${it.id}") })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getActiveShopId(): Flow<String> = preferences.activeShopId

    override suspend fun setActiveShopId(shopId: String) {
        preferences.setActiveShopId(shopId)
    }
}
