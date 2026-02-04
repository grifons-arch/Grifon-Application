package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class HomeViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    searchProductsUseCase: SearchProductsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeState>> = _uiState

    init {
        val shopFlow = getActiveShopUseCase()
        val productsFlow = shopFlow.flatMapLatest { shopId ->
            searchProductsUseCase(shopId, "", HomeState.defaultFilters, HomeState.defaultSort)
        }
        combine(shopFlow, productsFlow) { shopId, products ->
            HomeState(
                shopId = shopId,
                banners = listOf("Back to school", "Mega Sale"),
                popular = products.take(3),
                recent = products.takeLast(3),
            )
        }.onEach { state ->
            _uiState.value = UiState.Success(state)
        }.launchIn(viewModelScope)
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
