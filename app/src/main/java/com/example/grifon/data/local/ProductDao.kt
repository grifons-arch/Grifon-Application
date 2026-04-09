package com.example.grifon.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE shopId = :shopId")
    fun observeProductsByShop(shopId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND shopId = :shopId")
    fun observeProductsByCategory(categoryId: String, shopId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId AND shopId = :shopId")
    suspend fun getProductById(productId: String, shopId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE shopId = :shopId")
    suspend fun clearShop(shopId: String)
}
