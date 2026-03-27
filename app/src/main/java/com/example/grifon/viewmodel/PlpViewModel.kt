package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiEvent
import com.example.grifon.core.UiState
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.local.LocalPriceAccessService
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetProductsByCategoryUseCase
import com.example.grifon.domain.usecase.ObserveFavoritesUseCase
import com.example.grifon.domain.usecase.SearchProductsUseCase
import com.example.grifon.domain.usecase.ToggleFavoriteUseCase
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
    private val observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getActiveShopUseCase: GetActiveShopUseCase,
    private val shopPreferences: ShopPreferences,
    private val localPriceAccessService: LocalPriceAccessService,
) : ViewModel() {
    private val _filters = MutableStateFlow(FilterState())
    private val _sortOption = MutableStateFlow(SortOption.RELEVANCE)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("")
    private val _uiState = MutableStateFlow<UiState<PlpState>>(UiState.Loading)
    val uiState: StateFlow<UiState<PlpState>> = _uiState

    val events = MutableSharedFlow<UiEvent>()

    init {
        val sessionFlow = combine(
            getActiveShopUseCase(),
            shopPreferences.currentCustomerId,
            shopPreferences.canViewPrices,
        ) { shopId, customerId, canViewPrices ->
            PlpSessionState(
                shopId = ShopConfig.normalizeShopId(shopId),
                customerId = customerId,
                canViewPrices = canViewPrices,
            )
        }

        val baseQueryFlow = combine(
            sessionFlow,
            _query,
            _category,
            _filters,
        ) { session, query, category, filters ->
            SessionProductQuery(
                shopId = session.shopId,
                customerId = session.customerId,
                canViewPrices = session.canViewPrices,
                query = query,
                category = category,
                filters = filters,
            )
        }.flatMapLatest { session ->
            localPriceAccessService.observeCanDisplayPrices(
                shopId = session.shopId,
                customerId = session.customerId,
                canViewPrices = session.canViewPrices,
            ).map { canDisplayPrices ->
                BaseProductQuery(
                    shopId = session.shopId,
                    customerId = session.customerId,
                    canViewPrices = canDisplayPrices,
                    query = session.query,
                    category = session.category,
                    filters = session.filters,
                )
            }
        }

        combine(baseQueryFlow, _sortOption) { baseQuery, sortOption ->
            ProductQuery(
                shopId = baseQuery.shopId,
                customerId = baseQuery.customerId,
                canViewPrices = baseQuery.canViewPrices,
                query = baseQuery.query,
                category = baseQuery.category,
                filters = baseQuery.filters,
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
                    .flatMapLatest { products ->
                        observeFavoritesUseCase(params.shopId).map { favorites ->
                            products to favorites.map { it.productId }.toSet()
                        }
                    }
                    .map< Pair<List<Product>, Set<String>>, UiState<PlpState> > { (products, favoriteIds) ->
                        UiState.Success(
                            PlpState(
                                products = products,
                                favoriteIds = favoriteIds,
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

    fun toggleFavorite(product: Product) {
        viewModelScope.launch {
            val shopId = ShopConfig.normalizeShopId(getActiveShopUseCase().first())
            toggleFavoriteUseCase(shopId, product)
        }
    }
}

private data class ProductQuery(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
    val query: String,
    val category: String,
    val filters: FilterState,
    val sortOption: SortOption,
)

private data class BaseProductQuery(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
    val query: String,
    val category: String,
    val filters: FilterState,
)

private data class SessionProductQuery(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
    val query: String,
    val category: String,
    val filters: FilterState,
)

private data class PlpSessionState(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
)

data class PlpState(
    val products: List<Product>,
    val favoriteIds: Set<String>,
    val filters: FilterState,
    val sortOption: SortOption,
    val canLoadMore: Boolean = true
)
