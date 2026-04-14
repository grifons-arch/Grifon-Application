package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.CheckoutOrderResponseDto
import com.example.grifon.domain.model.LocalOrder
import com.example.grifon.domain.model.LocalOrderItem
import com.example.grifon.domain.usecase.ClearCartUseCase
import com.example.grifon.domain.usecase.SaveOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StripeCheckoutViewModel @Inject constructor(
    private val catalogApi: CatalogApi,
    private val clearCartUseCase: ClearCartUseCase,
    private val saveOrderUseCase: SaveOrderUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<StripeCheckoutState>>(UiState.Loading)
    val uiState: StateFlow<UiState<StripeCheckoutState>> = _uiState

    fun start(orderReference: String, checkoutUrl: String) {
        _uiState.value = UiState.Success(
            StripeCheckoutState(
                orderReference = orderReference,
                checkoutUrl = checkoutUrl,
                status = "approval_required",
                message = "Complete the Stripe card payment to continue.",
                isCompleting = false,
            )
        )
    }

    fun complete(orderReference: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (current.isCompleting) {
            return
        }

        _uiState.value = UiState.Success(current.copy(isCompleting = true))
        viewModelScope.launch {
            runCatching {
                catalogApi.captureCheckoutOrder(orderReference)
            }.onSuccess { response ->
                if (response.paymentSessionStatus == "paid") {
                    persistCompletedOrder(response)
                }
                _uiState.value = UiState.Success(
                    current.copy(
                        status = response.paymentSessionStatus,
                        message = response.paymentSessionMessage,
                        isCompleting = false,
                        completed = response.paymentSessionStatus == "paid",
                    )
                )
            }.onFailure { error ->
                _uiState.value = UiState.Error(
                    message = error.message ?: "Stripe verification failed.",
                    throwable = error
                )
            }
        }
    }

    private suspend fun persistCompletedOrder(response: CheckoutOrderResponseDto) {
        saveOrderUseCase(response.toLocalOrder())
        clearCartUseCase(response.shopId.toString())
    }

    fun markCancelled() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(
            current.copy(
                status = "cancelled",
                message = "Stripe payment was cancelled.",
                isCompleting = false
            )
        )
    }
}

private fun CheckoutOrderResponseDto.toLocalOrder(): LocalOrder {
    return LocalOrder(
        orderReference = orderReference,
        shopId = shopId.toString(),
        customerId = customerId,
        totalAmount = totalAmount,
        currency = currency,
        paymentProvider = paymentProvider,
        paymentMethodCode = paymentMethodCode,
        paymentStatus = paymentStatus,
        orderStatus = orderStatus,
        createdAt = System.currentTimeMillis(),
        items = items.map { item ->
            LocalOrderItem(
                productId = item.productId.toString(),
                title = item.title,
                qty = item.qty,
                unitPrice = item.unitPrice,
                currency = item.currency,
            )
        },
    )
}

data class StripeCheckoutState(
    val orderReference: String,
    val checkoutUrl: String,
    val status: String,
    val message: String,
    val isCompleting: Boolean,
    val completed: Boolean = false,
)
