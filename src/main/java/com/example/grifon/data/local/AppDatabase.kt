package com.example.grifon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        CategoryEntity::class,
        SubCategoryEntity::class,
        ProductEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
}
