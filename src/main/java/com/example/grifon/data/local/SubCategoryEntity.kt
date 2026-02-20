package com.example.grifon.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentId"])]
)
data class SubCategoryEntity(
    @PrimaryKey val id: String,
    val parentId: String, // ID της γονικής κατηγορίας
    val name: String,
    val position: Int,
    val active: Boolean,
    val shopId: String
)
