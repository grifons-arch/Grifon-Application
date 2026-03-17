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
            emit(response.map { shop ->
                val displayName = when(shop.code?.uppercase()) {
                    "GR" -> "Ελληνικό κατάστημα"
                    "SE" -> "Σουηδικό κατάστημα χονδρικής"
                    else -> shop.code ?: "Shop ${shop.id}"
                }
                Shop(id = shop.id.toString(), name = displayName)
            })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getActiveShopId(): Flow<String> = preferences.activeShopId

    override suspend fun setActiveShopId(shopId: String) {
        preferences.setActiveShopId(shopId)
    }
}
