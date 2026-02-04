package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiEvent
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetProductsByCategoryUseCase
import com.example.grifon.domain.usecase.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class PlpViewModel @Inject constructor(
    private val getProductsByCategoryUseCase: GetProductsByCategoryUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    getActiveShopUseCase: GetActiveShopUseCase,
) : ViewModel() {
    private val _filters = MutableStateFlow(FilterState())
    private val _sortOption = MutableStateFlow(SortOption.RELEVANCE)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("")
    private val _uiState = MutableStateFlow<UiState<PlpState>>(UiState.Loading)
    val uiState: StateFlow<UiState<PlpState>> = _uiState

    val events = MutableSharedFlow<UiEvent>()

    init {
        combine(getActiveShopUseCase(), _query, _category, _filters, _sortOption) {
            shopId, query, category, filters, sort ->
            PlpRequest(shopId, query, category, filters, sort)
        }.flatMapLatest { request ->
            if (request.category.isNotBlank()) {
                getProductsByCategoryUseCase(
                    request.shopId,
                    request.category,
                    request.filters,
                    request.sort,
                )
            } else {
                searchProductsUseCase(
                    request.shopId,
                    request.query,
                    request.filters,
                    request.sort,
                )
            }
        }.onEach { products ->
            _uiState.value = UiState.Success(
                PlpState(
                    products = products,
                    filters = _filters.value,
                    sortOption = _sortOption.value,
                )
            )
        }.launchIn(viewModelScope)
    }

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun updateCategory(categoryId: String) {
        _category.value = categoryId
    }

    fun updateFilters(filters: FilterState) {
        _filters.value = filters
    }

    fun updateSort(sortOption: SortOption) {
        _sortOption.value = sortOption
    }
}

data class PlpState(
    val products: List<Product>,
    val filters: FilterState,
    val sortOption: SortOption,
)

data class PlpRequest(
    val shopId: String,
    val query: String,
    val category: String,
    val filters: FilterState,
    val sort: SortOption,
)
