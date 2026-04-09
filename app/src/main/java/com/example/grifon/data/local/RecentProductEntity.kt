package com.example.grifon.data.local

import androidx.room.Entity

@Entity(
    tableName = "recently_visited",
    primaryKeys = ["customerId", "shopId", "productId"],
)
data class RecentProductEntity(
    val customerId: Int,
    val productId: String,
    val shopId: String,
    val title: String,
    val price: Double?,
    val currency: String,
    val imageUrl: String,
    val brand: String,
    val visitedAt: Long,
)
