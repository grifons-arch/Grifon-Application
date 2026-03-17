package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.model.Shop
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.SetActiveShopUseCase
import com.example.grifon.data.repository.ShopRepository
import com.example.grifon.data.local.ShopPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    shopRepository: ShopRepository,
    getActiveShopUseCase: GetActiveShopUseCase,
    private val setActiveShopUseCase: SetActiveShopUseCase,
    private val shopPreferences: ShopPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<SettingsState>>(UiState.Loading)
    val uiState: StateFlow<UiState<SettingsState>> = _uiState

    init {
        combine(
            shopRepository.getShops(), 
            getActiveShopUseCase(),
            shopPreferences.isDarkModeEnabled,
            shopPreferences.appLanguage
        ) { shops, activeId, darkMode, languageCode ->
            val normalizedActiveId = ShopConfig.normalizeShopId(activeId)
            SettingsState(
                shops = shops,
                activeShopId = normalizedActiveId,
                activeShopName = shops.firstOrNull {
                    ShopConfig.normalizeShopId(it.id) == normalizedActiveId
                }?.name ?: ShopConfig.displayName(normalizedActiveId),
                language = languageCode,
                currency = "EUR",
                darkMode = darkMode,
                notificationsEnabled = true,
            )
        }.onEach { state ->
            _uiState.value = UiState.Success(state)
        }.launchIn(viewModelScope)
    }

    fun setActiveShop(shop: Shop) {
        viewModelScope.launch {
            setActiveShopUseCase(shop.id)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            shopPreferences.setDarkModeEnabled(enabled)
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            shopPreferences.setLanguage(languageCode)
        }
    }
}

data class SettingsState(
    val shops: List<Shop>,
    val activeShopId: String,
    val activeShopName: String,
    val language: String,
    val currency: String,
    val darkMode: Boolean,
    val notificationsEnabled: Boolean,
)
