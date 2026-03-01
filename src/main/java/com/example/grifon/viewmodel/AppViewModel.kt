package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class AppViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    getCartUseCase: GetCartUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    init {
        getActiveShopUseCase()
            .flatMapLatest { shopId ->
                getCartUseCase(shopId).combine(getActiveShopUseCase()) { cartItems, activeShop ->
                    AppState(activeShopId = activeShop, cartCount = cartItems.sumOf { it.qty })
                }
            }
            .onEach { appState -> _state.value = appState }
            .launchIn(viewModelScope)
    }
}

data class AppState(
    val activeShopId: String = "shop_a",
    val cartCount: Int = 0,
)
