package com.example.grifon.data.repository

import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.LocalOrder
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocalOrderRepository(
    private val preferences: ShopPreferences,
    moshi: Moshi,
) : OrderRepository {
    private val orderListAdapter: JsonAdapter<List<LocalOrder>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, LocalOrder::class.java)
    )

    override fun observeOrders(shopId: String, customerId: Int?): Flow<List<LocalOrder>> {
        return preferences.ordersJson.map { json ->
            readOrders(json)
                .filter { order ->
                    order.shopId == shopId && (customerId == null || order.customerId == customerId)
                }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun saveOrder(order: LocalOrder) {
        val existing = readOrders(preferences.ordersJson.first())
        val updated = listOf(order) + existing.filterNot { it.orderReference == order.orderReference }
        preferences.setOrdersJson(orderListAdapter.toJson(updated))
    }

    private fun readOrders(json: String?): List<LocalOrder> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching { orderListAdapter.fromJson(json).orEmpty() }
            .getOrDefault(emptyList())
    }
}
