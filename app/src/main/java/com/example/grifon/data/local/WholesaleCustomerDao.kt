package com.example.grifon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WholesaleCustomerDao {
    @Query(
        """
        SELECT * FROM wholesale_customers
        WHERE shopId = :shopId
        ORDER BY company ASC, lastName ASC, firstName ASC
        """
    )
    fun observeWholesaleCustomers(shopId: String): Flow<List<WholesaleCustomerEntity>>

    @Query(
        """
        SELECT * FROM wholesale_customers
        WHERE shopId = :shopId AND customerId = :customerId
        LIMIT 1
        """
    )
    suspend fun getWholesaleCustomer(shopId: String, customerId: Int): WholesaleCustomerEntity?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM wholesale_customers
            WHERE shopId = :shopId AND customerId = :customerId
        )
        """
    )
    fun observeIsWholesaleCustomer(shopId: String, customerId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(customers: List<WholesaleCustomerEntity>)

    @Query("DELETE FROM wholesale_customers WHERE shopId = :shopId")
    suspend fun clearShop(shopId: String)
}
