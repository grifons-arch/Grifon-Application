package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiEvent
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase
import com.example.grifon.domain.usecase.GetProductsByCategoryUseCase
import com.example.grifon.domain.usecase.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class PlpViewModel @Inject constructor(
    private val getProductsByCategoryUseCase: GetProductsByCategoryUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    private val getActiveShopUseCase: GetActiveShopUseCase,
    private val getCategoryTreeUseCase: GetCategoryTreeUseCase,
) : ViewModel() {
    private val _filters = MutableStateFlow(FilterState())
    private val _sortOption = MutableStateFlow(SortOption.RELEVANCE)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("")
    private val _currentPage = MutableStateFlow(1)
    private val _products = MutableStateFlow<List<Product>>(emptyList())

    private val _uiState = MutableStateFlow<UiState<PlpState>>(UiState.Loading)
    val uiState: StateFlow<UiState<PlpState>> = _uiState

    val events = MutableSharedFlow<UiEvent>()

    init {
        combine(_query, _category, _filters, _sortOption) { _, _, _, _ -> }
            .onEach {
                _currentPage.value = 1
                _products.value = emptyList()
                loadPage()
            }
            .launchIn(viewModelScope)
    }

    fun loadPage() {
        viewModelScope.launch {
            val shopId = getActiveShopUseCase().first()
            val page = _currentPage.value

            val categories = getCategoryTreeUseCase(shopId).first()
            val flow = if (_category.value.isNotBlank()) {
                getProductsByCategoryUseCase(shopId, _category.value, _filters.value, _sortOption.value)
            } else {
                searchProductsUseCase(shopId, _query.value, _filters.value, _sortOption.value)
            }

            flow.collect { newProducts ->
                if (page == 1) {
                    _products.value = newProducts
                } else {
                    _products.value = _products.value + newProducts
                }

                _uiState.value = UiState.Success(
                    PlpState(
                        products = _products.value,
                        filters = _filters.value,
                        sortOption = _sortOption.value,
                        selectedCategoryId = _category.value,
                        categories = categories,
                        canLoadMore = newProducts.size >= 20
                    )
                )
            }
        }
    }

    fun loadNextPage() {
        if (_uiState.value is UiState.Success) {
            _currentPage.value += 1
            loadPage()
        }
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
    val selectedCategoryId: String,
    val categories: List<Category>,
    val canLoadMore: Boolean = true
)
