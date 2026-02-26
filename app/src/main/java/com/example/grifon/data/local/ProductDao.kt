package com.example.grifon.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT DISTINCT * FROM products WHERE shopId = :shopId")
    fun getProductsByShop(shopId: String): Flow<List<ProductEntity>>

    @Query("SELECT DISTINCT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE shopId = :shopId")
    suspend fun clearProductsByShop(shopId: String)
    
    @Query("DELETE FROM products")
    suspend fun clearAll()
}
