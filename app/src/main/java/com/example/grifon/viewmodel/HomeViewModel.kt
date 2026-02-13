package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.catalog.HomeProductsWebService
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    private val homeProductsWebService: HomeProductsWebService,
    private val searchProductsUseCase: SearchProductsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeState>> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val shopId = getActiveShopUseCase().first()
            val products = runCatching { homeProductsWebService.fetchAllProductsFromShops() }
                .getOrDefault(emptyList())
                .ifEmpty {
                    searchProductsUseCase(
                        shopId,
                        "",
                        HomeState.defaultFilters,
                        HomeState.defaultSort,
                    ).first()
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

data class HomeState(
    val shopId: String,
    val banners: List<String>,
    val popular: List<Product>,
    val recent: List<Product>,
) {
    companion object {
        val defaultFilters = com.example.grifon.domain.model.FilterState()
        val defaultSort = com.example.grifon.domain.model.SortOption.RELEVANCE
    }
}
