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
    private val customerEmailKey = stringPreferencesKey("current_customer_email")
    private val customerFirstNameKey = stringPreferencesKey("current_customer_first_name")
    private val customerLastNameKey = stringPreferencesKey("current_customer_last_name")
    private val customerCompanyKey = stringPreferencesKey("current_customer_company")
    private val checkoutRecipientKey = stringPreferencesKey("checkout_recipient")
    private val checkoutPhoneKey = stringPreferencesKey("checkout_phone")
    private val checkoutCompanyKey = stringPreferencesKey("checkout_company")
    private val checkoutStreetKey = stringPreferencesKey("checkout_street")
    private val checkoutCityKey = stringPreferencesKey("checkout_city")
    private val checkoutPostalCodeKey = stringPreferencesKey("checkout_postal_code")
    private val checkoutCountryKey = stringPreferencesKey("checkout_country")
    private val ordersJsonKey = stringPreferencesKey("orders_json")

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

    val currentCustomerEmail: Flow<String?> = dataStore.data.map { preferences ->
        preferences[customerEmailKey]
    }

    val currentCustomerFirstName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[customerFirstNameKey]
    }

    val currentCustomerLastName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[customerLastNameKey]
    }

    val currentCustomerCompany: Flow<String?> = dataStore.data.map { preferences ->
        preferences[customerCompanyKey]
    }

    val checkoutRecipient: Flow<String?> = dataStore.data.map { preferences ->
        preferences[checkoutRecipientKey]
    }

    val checkoutPhone: Flow<String?> = dataStore.data.map { preferences ->
        preferences[checkoutPhoneKey]
    }

    val checkoutCompany: Flow<String?> = dataStore.data.map { preferences ->
        preferences[checkoutCompanyKey]
    }

    val checkoutStreet: Flow<String?> = dataStore.data.map { preferences ->
        preferences[checkoutStreetKey]
    }

    val checkoutCity: Flow<String?> = dataStore.data.map { preferences ->
        preferences[checkoutCityKey]
    }

    val checkoutPostalCode: Flow<String?> = dataStore.data.map { preferences ->
        preferences[checkoutPostalCodeKey]
    }

    val checkoutCountry: Flow<String?> = dataStore.data.map { preferences ->
        preferences[checkoutCountryKey]
    }

    val ordersJson: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ordersJsonKey]
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

    suspend fun setCustomerSession(
        customerId: Int,
        canViewPrices: Boolean,
        email: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        company: String? = null,
    ) {
        dataStore.edit { preferences ->
            preferences[customerIdKey] = customerId.toString()
            preferences[canViewPricesKey] = canViewPrices
            email?.let { preferences[customerEmailKey] = it }
            firstName?.let { preferences[customerFirstNameKey] = it }
            lastName?.let { preferences[customerLastNameKey] = it }
            company?.let { preferences[customerCompanyKey] = it }
        }
    }

    suspend fun clearCustomerSession() {
        dataStore.edit { preferences ->
            preferences.remove(customerIdKey)
            preferences.remove(canViewPricesKey)
            preferences.remove(customerEmailKey)
            preferences.remove(customerFirstNameKey)
            preferences.remove(customerLastNameKey)
            preferences.remove(customerCompanyKey)
        }
    }

    suspend fun setCheckoutDraft(
        recipient: String? = null,
        phone: String? = null,
        company: String? = null,
        street: String? = null,
        city: String? = null,
        postalCode: String? = null,
        country: String? = null,
    ) {
        dataStore.edit { preferences ->
            recipient?.let { preferences[checkoutRecipientKey] = it }
            phone?.let { preferences[checkoutPhoneKey] = it }
            company?.let { preferences[checkoutCompanyKey] = it }
            street?.let { preferences[checkoutStreetKey] = it }
            city?.let { preferences[checkoutCityKey] = it }
            postalCode?.let { preferences[checkoutPostalCodeKey] = it }
            country?.let { preferences[checkoutCountryKey] = it }
        }
    }

    suspend fun setOrdersJson(json: String) {
        dataStore.edit { preferences ->
            preferences[ordersJsonKey] = json
        }
    }
}
