package com.example.grifon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentProductDao {
    @Query(
        """
        SELECT * FROM recently_visited
        WHERE customerId = :customerId AND shopId = :shopId
        ORDER BY visitedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentProducts(customerId: Int, shopId: String, limit: Int): Flow<List<RecentProductEntity>>

    @Query(
        """
        SELECT * FROM recently_visited
        WHERE customerId = :customerId AND shopId = :shopId
        ORDER BY visitedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentProducts(customerId: Int, shopId: String, limit: Int): List<RecentProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentProduct(product: RecentProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentProducts(products: List<RecentProductEntity>)

    @Query(
        """
        DELETE FROM recently_visited
        WHERE customerId = :customerId AND shopId = :shopId
        AND productId NOT IN (
            SELECT productId FROM recently_visited
            WHERE customerId = :customerId AND shopId = :shopId
            ORDER BY visitedAt DESC
            LIMIT :keep
        )
        """
    )
    suspend fun trimRecentProducts(customerId: Int, shopId: String, keep: Int)

    @Query(
        """
        DELETE FROM recently_visited
        WHERE customerId = :customerId AND shopId = :shopId
        """
    )
    suspend fun clearRecentProducts(customerId: Int, shopId: String)
}
