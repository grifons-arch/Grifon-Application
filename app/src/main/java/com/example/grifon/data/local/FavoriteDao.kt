package com.example.grifon.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query(
        """
        SELECT * FROM favorites
        WHERE customerId = :customerId AND shopId = :shopId
        ORDER BY addedAt DESC
        """
    )
    fun observeFavorites(customerId: Int, shopId: String): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query(
        """
        DELETE FROM favorites
        WHERE customerId = :customerId AND shopId = :shopId AND productId = :productId
        """
    )
    suspend fun deleteFavorite(customerId: Int, shopId: String, productId: String)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM favorites
            WHERE customerId = :customerId AND shopId = :shopId AND productId = :productId
        )
        """
    )
    fun observeIsFavorite(customerId: Int, shopId: String, productId: String): Flow<Boolean>
}
