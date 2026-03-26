package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCartUseCase
import com.example.grifon.domain.usecase.RemoveFromCartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    private val getCartUseCase: GetCartUseCase,
    private val removeFromCartUseCase: RemoveFromCartUseCase,
    private val shopPreferences: ShopPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<CartState>>(UiState.Loading)
    val uiState: StateFlow<UiState<CartState>> = _uiState
    private val _shopId = MutableStateFlow("")

    init {
        combine(getActiveShopUseCase(), _shopId, shopPreferences.canViewPrices) { shopId, _, canViewPrices ->
            shopId to canViewPrices
        }.onEach { (shopId, canViewPrices) ->
            _shopId.value = shopId
            getCartUseCase(shopId)
                .onEach { items ->
                    _uiState.value = UiState.Success(
                        CartState(
                            items = items,
                            total = if (canViewPrices) items.sumOf { it.qty * it.priceSnapshot } else null,
                            canViewPrices = canViewPrices,
                        )
                    )
                }
                .launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }

    fun removeItem(item: CartItem) {
        viewModelScope.launch {
            removeFromCartUseCase(_shopId.value, item.productId)
        }
    }
}

data class CartState(
    val items: List<CartItem>,
    val total: Double?,
    val canViewPrices: Boolean,
)
