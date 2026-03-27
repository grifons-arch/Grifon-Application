package com.example.grifon.data.local

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["customerId", "shopId", "productId"],
)
data class FavoriteEntity(
    val customerId: Int,
    val productId: String,
    val shopId: String,
    val title: String,
    val price: Double?,
    val currency: String,
    val imageUrl: String,
    val brand: String,
    val addedAt: Long,
)
