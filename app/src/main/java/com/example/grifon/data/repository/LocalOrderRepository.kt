package com.example.grifon.data.repository

import com.example.grifon.core.ShopConfig
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.LocalOrder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalOrderRepository(
    private val preferences: ShopPreferences,
    moshi: Moshi,
) : OrderRepository {
    private val orderListAdapter = moshi.adapter<List<LocalOrder>>(
        Types.newParameterizedType(List::class.java, LocalOrder::class.java)
    )

    override fun observeOrders(shopId: String, customerId: Int?): Flow<List<LocalOrder>> {
        val normalizedShopId = ShopConfig.normalizeShopId(shopId)
        return preferences.ordersJson.map { json ->
            parseOrders(json)
                .filter { order ->
                    order.shopId == normalizedShopId &&
                        (customerId == null || order.customerId == customerId)
                }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun saveOrder(order: LocalOrder) {
        val orders = parseOrders(preferences.ordersJson.first())
            .filterNot { existing ->
                existing.shopId == order.shopId &&
                    existing.orderReference == order.orderReference
            } + order.copy(shopId = ShopConfig.normalizeShopId(order.shopId))

        preferences.setOrdersJson(orderListAdapter.toJson(orders))
    }

    private fun parseOrders(json: String?): List<LocalOrder> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching { orderListAdapter.fromJson(json).orEmpty() }
            .getOrDefault(emptyList())
    }
}
