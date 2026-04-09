package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.ShopConfig
import com.example.grifon.core.UiState
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.LocalOrder
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.ObserveOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrdersViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    observeOrdersUseCase: ObserveOrdersUseCase,
    shopPreferences: ShopPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<LocalOrder>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<LocalOrder>>> = _uiState

    init {
        combine(
            getActiveShopUseCase(),
            shopPreferences.currentCustomerId,
        ) { shopId, customerId ->
            ShopConfig.normalizeShopId(shopId) to customerId
        }.flatMapLatest { (shopId, customerId) ->
            observeOrdersUseCase(shopId, customerId)
        }.onEach { orders ->
            _uiState.value = UiState.Success(orders)
        }.launchIn(viewModelScope)
    }
}
