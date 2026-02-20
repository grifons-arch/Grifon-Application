package com.example.grifon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategories",
    indices = [Index(value = ["parentId"])]
)
data class SubCategoryEntity(
    @PrimaryKey val id: String,
    val parentId: String, 
    val name: String,
    val position: Int,
    val active: Boolean,
    val shopId: String
)
