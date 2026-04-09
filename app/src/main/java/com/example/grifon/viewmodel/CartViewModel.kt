package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.local.LocalPriceAccessService
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCartUseCase
import com.example.grifon.domain.usecase.RemoveFromCartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CartViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    private val getCartUseCase: GetCartUseCase,
    private val removeFromCartUseCase: RemoveFromCartUseCase,
    private val shopPreferences: ShopPreferences,
    private val localPriceAccessService: LocalPriceAccessService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<CartState>>(UiState.Loading)
    val uiState: StateFlow<UiState<CartState>> = _uiState
    private val _shopId = MutableStateFlow("")

    init {
        combine(
            getActiveShopUseCase(),
            shopPreferences.currentCustomerId,
            shopPreferences.canViewPrices,
        ) { shopId, customerId, canViewPrices ->
            CartSessionState(shopId = shopId, customerId = customerId, canViewPrices = canViewPrices)
        }.flatMapLatest { session ->
            val normalizedShopId = session.shopId
            localPriceAccessService.observeCanDisplayPrices(
                shopId = normalizedShopId,
                customerId = session.customerId,
                canViewPrices = session.canViewPrices,
            ).flatMapLatest { canDisplayPrices ->
                _shopId.value = normalizedShopId
                getCartUseCase(normalizedShopId).map { items ->
                    UiState.Success(
                        CartState(
                            shopId = normalizedShopId,
                            items = items,
                            total = if (canDisplayPrices) items.sumOf { it.qty * it.priceSnapshot } else null,
                            canViewPrices = canDisplayPrices,
                            currency = items.firstOrNull()?.currency,
                        )
                    )
                }
            }
        }.onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun removeItem(item: CartItem) {
        viewModelScope.launch {
            removeFromCartUseCase(_shopId.value, item.productId)
        }
    }
}

data class CartState(
    val shopId: String,
    val items: List<CartItem>,
    val total: Double?,
    val canViewPrices: Boolean,
    val currency: String?,
)

private data class CartSessionState(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
)
