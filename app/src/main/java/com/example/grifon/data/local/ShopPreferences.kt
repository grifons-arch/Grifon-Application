package com.example.grifon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShopPreferences(private val dataStore: DataStore<Preferences>) {
    private val shopKey = stringPreferencesKey("active_shop_id")
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")

    val activeShopId: Flow<String> = dataStore.data.map { preferences ->
        preferences[shopKey] ?: "shop_gr"
    }

    val isDarkModeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[darkModeKey] ?: false
    }

    suspend fun setActiveShopId(shopId: String) {
        dataStore.edit { preferences ->
            preferences[shopKey] = shopId
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[darkModeKey] = enabled
        }
    }
}
