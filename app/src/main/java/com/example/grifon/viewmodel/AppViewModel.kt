package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCartUseCase
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
    shopPreferences: ShopPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    init {
        combine(
            getActiveShopUseCase(),
            shopPreferences.appLanguage,
        ) { activeId, languageCode ->
            ShopConfig.normalizeShopId(activeId) to languageCode
        }.flatMapLatest { (activeId, _languageCode) ->
            getCartUseCase(activeId).map { cartItems ->
                AppState(
                    activeShopId = activeId,
                    shopName = ShopConfig.displayName(activeId),
                    cartCount = cartItems.sumOf { it.qty },
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
)
