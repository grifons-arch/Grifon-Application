package com.example.grifon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String, // format: shopId_productId
    val title: String,
    val price: Double,
    val currency: String,
    val imageUrl: String,
    val brand: String,
    val inStock: Boolean,
    val reference: String,
    val shopId: String,
    val categoryId: String? = null
)
