package com.example.grifon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val price: Double,
    val imageUrl: String,
    val shopId: String
)
