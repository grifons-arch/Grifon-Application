package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiEvent
import com.example.grifon.core.UiState
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.AddToCartUseCase
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetProductByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PdpViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val addToCartUseCase: AddToCartUseCase,
) : ViewModel() {
    private val _productId = MutableStateFlow("")
    private val _shopId = MutableStateFlow("")
    private val _uiState = MutableStateFlow<UiState<Product>>(UiState.Loading)
    val uiState: StateFlow<UiState<Product>> = _uiState
    val events = MutableSharedFlow<UiEvent>()

    init {
        combine(getActiveShopUseCase(), _productId) { shopId, productId ->
            ShopConfig.normalizeShopId(shopId) to productId
        }.distinctUntilChanged()
            .flatMapLatest { (shopId, productId) ->
                _shopId.value = shopId
                if (productId.isBlank()) {
                    kotlinx.coroutines.flow.flowOf(UiState.Loading)
                } else {
                    getProductByIdUseCase(shopId, productId)
                        .map { product ->
                            if (product != null) {
                                UiState.Success(product)
                            } else {
                                UiState.Error("Product not found")
                            }
                        }
                        .onStart { emit(UiState.Loading) }
                }
            }
            .onEach { state -> _uiState.value = state }
            .launchIn(viewModelScope)
    }

    fun setProductId(productId: String) {
        _productId.value = productId
    }

    fun addToCart(product: Product) {
        val price = product.price ?: return
        viewModelScope.launch {
            addToCartUseCase(_shopId.value, CartItem(product.id, 1, price))
            events.emit(UiEvent.ShowSnackbar("Προστέθηκε στο καλάθι"))
        }
    }
}
