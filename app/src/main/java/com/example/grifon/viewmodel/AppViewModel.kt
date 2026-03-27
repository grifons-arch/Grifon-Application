package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCartUseCase
import com.example.grifon.domain.usecase.ObserveFavoritesUseCase
import com.example.grifon.data.local.ShopPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    getCartUseCase: GetCartUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    shopPreferences: ShopPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    init {
        combine(
            getActiveShopUseCase(),
            shopPreferences.appLanguage,
            shopPreferences.currentCustomerId,
            shopPreferences.canViewPrices,
        ) { activeId, languageCode, customerId, canViewPrices ->
            SessionAwareAppState(
                activeShopId = ShopConfig.normalizeShopId(activeId),
                languageCode = languageCode,
                canDisplayPrices = customerId != null && canViewPrices,
                isLoggedIn = customerId != null,
            )
        }.flatMapLatest { sessionState ->
            combine(
                getCartUseCase(sessionState.activeShopId),
                observeFavoritesUseCase(sessionState.activeShopId),
            ) { cartItems, favorites ->
                AppState(
                    activeShopId = sessionState.activeShopId,
                    shopName = ShopConfig.displayName(sessionState.activeShopId),
                    cartCount = cartItems.sumOf { it.qty },
                    favoriteCount = favorites.size,
                    canDisplayPrices = sessionState.canDisplayPrices,
                    isLoggedIn = sessionState.isLoggedIn,
                )
            }
        }
        .onEach { _state.value = it }
        .launchIn(viewModelScope)
    }
}

data class AppState(
    val activeShopId: String = "",
    val shopName: String = "",
    val cartCount: Int = 0,
    val favoriteCount: Int = 0,
    val canDisplayPrices: Boolean = false,
    val isLoggedIn: Boolean = false,
)

private data class SessionAwareAppState(
    val activeShopId: String,
    val languageCode: String,
    val canDisplayPrices: Boolean,
    val isLoggedIn: Boolean,
)
