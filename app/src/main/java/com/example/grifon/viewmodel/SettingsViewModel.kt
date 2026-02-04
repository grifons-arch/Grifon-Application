package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Shop
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.SetActiveShopUseCase
import com.example.grifon.data.repository.ShopRepository
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
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<SettingsState>>(UiState.Loading)
    val uiState: StateFlow<UiState<SettingsState>> = _uiState

    init {
        combine(shopRepository.getShops(), getActiveShopUseCase()) { shops, activeId ->
            SettingsState(
                shops = shops,
                activeShopId = activeId,
                language = "Ελληνικά",
                currency = "EUR",
                darkMode = false,
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
}

data class SettingsState(
    val shops: List<Shop>,
    val activeShopId: String,
    val language: String,
    val currency: String,
    val darkMode: Boolean,
    val notificationsEnabled: Boolean,
)
