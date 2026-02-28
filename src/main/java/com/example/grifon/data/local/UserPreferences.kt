package com.example.grifon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences(private val dataStore: DataStore<Preferences>) {
    private val tokenKey = stringPreferencesKey("auth_token")
    private val customerIdKey = stringPreferencesKey("customer_id")

    val authToken: Flow<String?> = dataStore.data.map { it[tokenKey] }
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[tokenKey] != null }

    suspend fun saveAuthData(token: String, customerId: String) {
        dataStore.edit { preferences ->
            preferences[tokenKey] = token
            preferences[customerIdKey] = customerId
        }
    }

    suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences.remove(tokenKey)
            preferences.remove(customerIdKey)
        }
    }
}
