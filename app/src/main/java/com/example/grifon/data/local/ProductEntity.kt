package com.example.grifon.data.local

import androidx.room.Entity

@Entity(
    tableName = "products",
    primaryKeys = ["id", "shopId"],
)
data class ProductEntity(
    val id: String,
    val title: String,
    val price: Double?,
    val currency: String,
    val imageUrl: String,
    val brand: String,
    val inStock: Boolean,
    val reference: String,
    val shopId: String,
    val categoryId: String? = null,
    val active: Boolean,
    val syncedAt: Long,
)
