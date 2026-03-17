package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiEvent
import com.example.grifon.core.UiState
import com.example.grifon.core.ShopConfig
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetProductsByCategoryUseCase
import com.example.grifon.domain.usecase.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlpViewModel @Inject constructor(
    private val getProductsByCategoryUseCase: GetProductsByCategoryUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    private val getActiveShopUseCase: GetActiveShopUseCase,
) : ViewModel() {
    private val _filters = MutableStateFlow(FilterState())
    private val _sortOption = MutableStateFlow(SortOption.RELEVANCE)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("")
    private val _uiState = MutableStateFlow<UiState<PlpState>>(UiState.Loading)
    val uiState: StateFlow<UiState<PlpState>> = _uiState

    val events = MutableSharedFlow<UiEvent>()

    init {
        combine(
            getActiveShopUseCase(),
            _query,
            _category,
            _filters,
            _sortOption,
        ) { shopId, query, category, filters, sortOption ->
            ProductQuery(
                shopId = ShopConfig.normalizeShopId(shopId),
                query = query,
                category = category,
                filters = filters,
                sortOption = sortOption,
            )
        }.distinctUntilChanged()
            .flatMapLatest { params ->
                val source = if (params.category.isNotBlank()) {
                    getProductsByCategoryUseCase(
                        params.shopId,
                        params.category,
                        params.filters,
                        params.sortOption,
                    )
                } else {
                    searchProductsUseCase(
                        params.shopId,
                        params.query,
                        params.filters,
                        params.sortOption,
                    )
                }

                source
                    .map< List<Product>, UiState<PlpState> > { products ->
                        UiState.Success(
                            PlpState(
                                products = products,
                                filters = params.filters,
                                sortOption = params.sortOption,
                                canLoadMore = false,
                            )
                        )
                    }
                    .onStart { emit(UiState.Loading) }
            }
            .onEach { state -> _uiState.value = state }
            .launchIn(viewModelScope)
    }

    fun updateQuery(query: String) { _query.value = query }
    fun updateCategory(categoryId: String) { _category.value = categoryId }
    fun updateFilters(filters: FilterState) { _filters.value = filters }
    fun updateSort(sortOption: SortOption) { _sortOption.value = sortOption }
}

private data class ProductQuery(
    val shopId: String,
    val query: String,
    val category: String,
    val filters: FilterState,
    val sortOption: SortOption,
)

data class PlpState(
    val products: List<Product>,
    val filters: FilterState,
    val sortOption: SortOption,
    val canLoadMore: Boolean = true
)
