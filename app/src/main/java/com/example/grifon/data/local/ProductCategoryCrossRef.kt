package com.example.grifon.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "product_category_cross_ref",
    primaryKeys = ["productId", "categoryId"],
    indices = [Index(value = ["categoryId"])]
)
data class ProductCategoryCrossRef(
    val productId: String,
    val categoryId: String
)
