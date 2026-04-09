package com.example.grifon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String, // format: shopId_categoryId
    val name: String,
    val parentId: String?, // format: shopId_parentId
    val position: Int,
    val active: Boolean,
    val shopId: String
)
