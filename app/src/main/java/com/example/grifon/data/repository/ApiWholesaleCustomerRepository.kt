package com.example.grifon.data.repository

import androidx.room.withTransaction
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.WholesaleCustomerDto
import com.example.grifon.data.local.AppDatabase
import com.example.grifon.data.local.WholesaleCustomerEntity
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray

@Singleton
class ApiWholesaleCustomerRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val catalogApi: CatalogApi,
) : WholesaleCustomerRepository {

    override suspend fun syncWholesaleCustomers(shopId: String): Int {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        val response = catalogApi.getWholesaleCustomers(shopId = normalizedShopId.toInt())
        val syncedAt = System.currentTimeMillis()
        val entities = response.items.map { it.toEntity(normalizedShopId, syncedAt) }

        appDatabase.withTransaction {
            appDatabase.wholesaleCustomerDao().clearShop(normalizedShopId)
            if (entities.isNotEmpty()) {
                appDatabase.wholesaleCustomerDao().upsertAll(entities)
            }
        }

        return entities.size
    }
}

private fun WholesaleCustomerDto.toEntity(shopId: String, syncedAt: Long): WholesaleCustomerEntity {
    return WholesaleCustomerEntity(
        customerId = customerId,
        shopId = shopId,
        email = email.orEmpty(),
        firstName = firstName.orEmpty(),
        lastName = lastName.orEmpty(),
        company = company,
        active = active,
        defaultGroupId = defaultGroupId,
        defaultGroupName = defaultGroupName,
        wholesaleGroupIdsJson = JSONArray(wholesaleGroupIds).toString(),
        wholesaleGroupNamesJson = JSONArray(wholesaleGroupNames).toString(),
        syncedAt = syncedAt,
    )
}
