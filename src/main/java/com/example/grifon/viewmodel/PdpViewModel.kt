package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiEvent
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.AddToCartUseCase
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetProductByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
            shopId to productId
        }.onEach { (shopId, productId) ->
            _shopId.value = shopId
            if (productId.isNotBlank()) {
                getProductByIdUseCase(shopId, productId)
                    .onEach { product ->
                        if (product != null) {
                            _uiState.value = UiState.Success(product)
                        } else {
                            _uiState.value = UiState.Error("Product not found")
                        }
                    }
                    .launchIn(viewModelScope)
            }
        }.launchIn(viewModelScope)
    }

    fun setProductId(productId: String) {
        _productId.value = productId
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            addToCartUseCase(_shopId.value, CartItem(product.id, 1, product.price))
            events.emit(UiEvent.ShowSnackbar("Προστέθηκε στο καλάθι"))
        }
    }
}
