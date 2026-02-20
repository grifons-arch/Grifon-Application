package com.example.grifon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShopPreferences(private val dataStore: DataStore<Preferences>) {
    private val shopKey = stringPreferencesKey("active_shop_id")

    val activeShopId: Flow<String> = dataStore.data.map { preferences ->
        preferences[shopKey] ?: "4" // Default to Shop 4 (GR)
    }

    suspend fun setActiveShopId(shopId: String) {
        dataStore.edit { preferences ->
            preferences[shopKey] = shopId
        }
    }
}
