package com.example.grifon.data.local

import androidx.room.Entity

@Entity(
    tableName = "customers",
    primaryKeys = ["customerId", "shopId"],
)
data class CustomerEntity(
    val customerId: Int,
    val shopId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val company: String?,
    val active: Boolean,
    val defaultGroupId: Int?,
    val defaultGroupName: String?,
    val groupIdsJson: String,
    val groupNamesJson: String,
    val wholesaleGroupIdsJson: String,
    val wholesaleGroupNamesJson: String,
    val isWholesale: Boolean,
    val syncedAt: Long,
)
