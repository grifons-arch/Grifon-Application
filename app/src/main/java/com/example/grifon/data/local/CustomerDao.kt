package com.example.grifon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query(
        """
        SELECT * FROM customers
        WHERE shopId = :shopId
        ORDER BY company ASC, lastName ASC, firstName ASC
        """
    )
    fun observeCustomers(shopId: String): Flow<List<CustomerEntity>>

    @Query(
        """
        SELECT * FROM customers
        WHERE shopId = :shopId AND customerId = :customerId
        LIMIT 1
        """
    )
    suspend fun getCustomer(shopId: String, customerId: Int): CustomerEntity?

    @Query(
        """
        SELECT * FROM customers
        WHERE shopId = :shopId AND customerId = :customerId
        LIMIT 1
        """
    )
    fun observeCustomer(shopId: String, customerId: Int): Flow<CustomerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers WHERE shopId = :shopId")
    suspend fun clearShop(shopId: String)
}
