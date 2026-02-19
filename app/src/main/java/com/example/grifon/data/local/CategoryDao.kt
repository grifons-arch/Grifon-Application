package com.example.grifon.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE shopId = :shopId")
    fun getCategoriesByShop(shopId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE shopId = :shopId")
    suspend fun clearCategoriesByShop(shopId: String)
}
