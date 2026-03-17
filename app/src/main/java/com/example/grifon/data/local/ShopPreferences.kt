package com.example.grifon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.grifon.BuildConfig
import com.example.grifon.core.ShopConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShopPreferences(private val dataStore: DataStore<Preferences>) {
    private val shopKey = stringPreferencesKey("active_shop_id")
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val languageKey = stringPreferencesKey("app_language")

    val activeShopId: Flow<String> = dataStore.data.map { preferences ->
        ShopConfig.normalizeShopId(preferences[shopKey] ?: BuildConfig.SHOP_ID)
    }

    val isDarkModeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[darkModeKey] ?: false
    }

    val appLanguage: Flow<String> = dataStore.data.map { preferences ->
        preferences[languageKey] ?: "el" // Default στα Ελληνικά
    }

    suspend fun setActiveShopId(shopId: String) {
        dataStore.edit { preferences ->
            preferences[shopKey] = ShopConfig.normalizeShopId(shopId)
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[darkModeKey] = enabled
        }
    }

    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[languageKey] = languageCode
        }
    }
}
