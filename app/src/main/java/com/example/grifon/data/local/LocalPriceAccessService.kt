package com.example.grifon.data.local

import com.example.grifon.core.ShopConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.json.JSONArray

@Singleton
class LocalPriceAccessService @Inject constructor(
    private val customerDao: CustomerDao,
) {
    fun observeCanDisplayPrices(
        shopId: String,
        customerId: Int?,
        canViewPrices: Boolean,
    ): Flow<Boolean> {
        if (customerId == null) {
            return flowOf(false)
        }
        if (canViewPrices) {
            return flowOf(true)
        }
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        return customerDao.observeCustomer(normalizedShopId, customerId).map { customer ->
            customer?.hasWholesaleGroup() == true
        }
    }

    suspend fun canDisplayPrices(
        shopId: String,
        customerId: Int?,
        canViewPrices: Boolean,
    ): Boolean {
        if (customerId == null) {
            return false
        }
        if (canViewPrices) {
            return true
        }
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        return customerDao.getCustomer(normalizedShopId, customerId)?.hasWholesaleGroup() == true
    }
}

private fun CustomerEntity.hasWholesaleGroup(): Boolean {
    if (!active) {
        return false
    }

    if (isWholesale) {
        return true
    }

    val allGroupNames = buildList {
        defaultGroupName?.let(::add)
        addAll(parseJsonStringArray(groupNamesJson))
        addAll(parseJsonStringArray(wholesaleGroupNamesJson))
    }

    return allGroupNames.any { groupName ->
        groupName.contains("wholesale", ignoreCase = true)
    }
}

private fun parseJsonStringArray(json: String): List<String> {
    return runCatching {
        val array = JSONArray(json)
        List(array.length()) { index -> array.optString(index) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }.getOrDefault(emptyList())
}
