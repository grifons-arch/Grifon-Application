package com.example.grifon.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE shopId = :shopId")
    fun getCategoriesByShop(shopId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductCategoryRefs(refs: List<ProductCategoryCrossRef>)

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    @Transaction
    @Query("""
        SELECT DISTINCT p.* FROM products p
        INNER JOIN product_category_cross_ref ref ON p.id = ref.productId
        WHERE ref.categoryId = :categoryId
    """)
    fun getCategoryWithProducts(categoryId: String): Flow<List<ProductEntity>>
    
    @Query("DELETE FROM categories")
    suspend fun clearAll()
}
