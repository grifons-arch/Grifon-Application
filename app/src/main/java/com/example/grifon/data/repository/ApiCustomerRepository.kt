package com.example.grifon.data.repository

import androidx.room.withTransaction
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.CustomerDto
import com.example.grifon.data.local.AppDatabase
import com.example.grifon.data.local.CustomerEntity
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray

@Singleton
class ApiCustomerRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val catalogApi: CatalogApi,
) : CustomerRepository {

    override suspend fun syncCustomers(shopId: String): Int {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        val response = catalogApi.getCustomers(shopId = normalizedShopId.toInt())
        val syncedAt = System.currentTimeMillis()
        val entities = response.items.map { it.toEntity(normalizedShopId, syncedAt) }

        appDatabase.withTransaction {
            appDatabase.customerDao().clearShop(normalizedShopId)
            if (entities.isNotEmpty()) {
                appDatabase.customerDao().upsertAll(entities)
            }
        }

        return entities.size
    }
}

private fun CustomerDto.toEntity(shopId: String, syncedAt: Long): CustomerEntity {
    return CustomerEntity(
        customerId = customerId,
        shopId = shopId,
        email = email.orEmpty(),
        firstName = firstName.orEmpty(),
        lastName = lastName.orEmpty(),
        company = company,
        active = active,
        defaultGroupId = defaultGroupId,
        defaultGroupName = defaultGroupName,
        groupIdsJson = JSONArray(groupIds).toString(),
        groupNamesJson = JSONArray(groupNames).toString(),
        wholesaleGroupIdsJson = JSONArray(wholesaleGroupIds).toString(),
        wholesaleGroupNamesJson = JSONArray(wholesaleGroupNames).toString(),
        isWholesale = isWholesale,
        syncedAt = syncedAt,
    )
}
