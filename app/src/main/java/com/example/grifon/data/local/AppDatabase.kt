package com.example.grifon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        RecentProductEntity::class,
        WholesaleCustomerEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentProductDao(): RecentProductDao
    abstract fun wholesaleCustomerDao(): WholesaleCustomerDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
}
