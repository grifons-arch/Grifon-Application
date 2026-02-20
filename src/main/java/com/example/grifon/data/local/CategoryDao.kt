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

    @Query("SELECT * FROM subcategories")
    fun getAllSubCategories(): Flow<List<SubCategoryEntity>>

    @Query("SELECT * FROM subcategories WHERE parentId = :parentId")
    fun getSubCategories(parentId: String): Flow<List<SubCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategories(subCategories: List<SubCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductCategoryRefs(refs: List<ProductCategoryCrossRef>)

    @Transaction
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryWithSubCategories(categoryId: String): Flow<CategoryWithSubCategories>
}

data class CategoryWithSubCategories(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val subCategories: List<SubCategoryEntity>
)
