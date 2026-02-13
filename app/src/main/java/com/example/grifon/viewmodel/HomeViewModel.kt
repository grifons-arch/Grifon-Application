package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.catalog.HomeProductsWebService
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    private val homeProductsWebService: HomeProductsWebService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeState>> = _uiState

    init {
        viewModelScope.launch {
            getActiveShopUseCase()
                .distinctUntilChanged()
                .collect { shopId ->
                    _uiState.value = UiState.Loading

                    val products = runCatching {
                        homeProductsWebService.fetchProductsForShop(shopId)
                    }.getOrElse { error ->
                        _uiState.value = UiState.Error(
                            error.message ?: "Αποτυχία φόρτωσης προϊόντων από PrestaShop",
                        )
                        return@collect
                    }

                    if (products.isEmpty()) {
                        _uiState.value = UiState.Error(
                            "Δεν βρέθηκαν προϊόντα PrestaShop για το επιλεγμένο κατάστημα",
                        )
                        return@collect
                    }

                    _uiState.value = UiState.Success(
                        HomeState(
                            shopId = shopId,
                            banners = listOf("Back to school", "Mega Sale"),
                            popular = products.take(10),
                            recent = products.takeLast(10),
                        ),
                    )
                }
        }
    }
}

data class HomeState(
    val shopId: String,
    val banners: List<String>,
    val popular: List<Product>,
    val recent: List<Product>,
)
