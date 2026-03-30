package com.example.grifon.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.grifon.core.AppLanguage
import com.example.grifon.BuildConfig
import com.example.grifon.core.ShopConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShopPreferences(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    private val shopKey = stringPreferencesKey("active_shop_id")
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val languageKey = stringPreferencesKey("app_language")
    private val customerIdKey = stringPreferencesKey("current_customer_id")
    private val canViewPricesKey = booleanPreferencesKey("can_view_prices")

    // Default to "4" (Grifon GR)
    val activeShopId: Flow<String> = dataStore.data.map { preferences ->
        ShopConfig.normalizeShopId(preferences[shopKey] ?: BuildConfig.SHOP_ID)
    }

    val isDarkModeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[darkModeKey] ?: false
    }

    val appLanguage: Flow<String> = dataStore.data.map { preferences ->
        AppLanguage.normalize(preferences[languageKey] ?: AppLanguage.getStoredLanguage(context))
    }

    val currentCustomerId: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[customerIdKey]?.toIntOrNull()
    }

    val canViewPrices: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[canViewPricesKey] ?: false
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
        val normalizedLanguage = AppLanguage.normalize(languageCode)
        AppLanguage.persist(context, normalizedLanguage)
        dataStore.edit { preferences ->
            preferences[languageKey] = normalizedLanguage
        }
    }

    suspend fun setCustomerSession(customerId: Int, canViewPrices: Boolean) {
        dataStore.edit { preferences ->
            preferences[customerIdKey] = customerId.toString()
            preferences[canViewPricesKey] = canViewPrices
        }
    }

    suspend fun clearCustomerSession() {
        dataStore.edit { preferences ->
            preferences.remove(customerIdKey)
            preferences.remove(canViewPricesKey)
        }
    }
}
