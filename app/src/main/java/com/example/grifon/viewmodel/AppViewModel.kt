package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCartUseCase
import com.example.grifon.data.repository.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    getCartUseCase: GetCartUseCase,
    shopRepository: ShopRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    init {
        combine(
            getActiveShopUseCase(),
            shopRepository.getShops()
        ) { activeId, shops ->
            ShopConfig.normalizeShopId(activeId) to shops
        }.flatMapLatest { (activeId, shops) ->
            getCartUseCase(activeId).map { cartItems ->
                val shopName = shops.find {
                    ShopConfig.normalizeShopId(it.id) == activeId
                }?.name ?: ShopConfig.displayName(activeId)
                AppState(
                    activeShopId = activeId,
                    shopName = shopName,
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
    val shopName: String = "Φόρτωση...",
    val cartCount: Int = 0,
)
