package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiEvent
import com.example.grifon.core.UiState
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.local.LocalPriceAccessService
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.CatalogFacet
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCategoryFiltersUseCase
import com.example.grifon.domain.usecase.GetProductsByCategoryUseCase
import com.example.grifon.domain.usecase.ObserveFavoritesUseCase
import com.example.grifon.domain.usecase.SearchProductsUseCase
import com.example.grifon.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlpViewModel @Inject constructor(
    private val getProductsByCategoryUseCase: GetProductsByCategoryUseCase,
    private val getCategoryFiltersUseCase: GetCategoryFiltersUseCase,
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
            shopPreferences.appLanguage,
        ) { shopId, customerId, canViewPrices, languageCode ->
            PlpSessionState(
                shopId = ShopConfig.normalizeShopId(shopId),
                customerId = customerId,
                canViewPrices = canViewPrices,
                languageCode = languageCode,
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
                languageCode = session.languageCode,
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
                    languageCode = session.languageCode,
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
                languageCode = baseQuery.languageCode,
                query = baseQuery.query,
                category = baseQuery.category,
                filters = baseQuery.filters,
                sortOption = sortOption,
            )
        }.distinctUntilChanged()
            .flatMapLatest { params ->
                val hasSearchQuery = params.query.isNotBlank()
                val source = if (params.category.isNotBlank()) {
                    getProductsByCategoryUseCase(
                        params.shopId,
                        params.category,
                        params.filters,
                        params.sortOption,
                    ).map { products ->
                        if (hasSearchQuery) {
                            val filtered = products.filter { product ->
                                product.matchesPlpQuery(params.query)
                            }
                            if (filtered.isNotEmpty()) filtered else products
                        } else {
                            products
                        }
                    }
                } else if (hasSearchQuery) {
                    searchProductsUseCase(
                        params.shopId,
                        params.query,
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

                val facetsSource = if (params.category.isNotBlank()) {
                    getCategoryFiltersUseCase(params.shopId, params.category)
                } else {
                    flowOf(emptyList())
                }

                combine(source, facetsSource, observeFavoritesUseCase(params.shopId)) { products, facets, favorites ->
                    Triple(products, facets, favorites.map { it.productId }.toSet())
                }
                    .map<Triple<List<Product>, List<CatalogFacet>, Set<String>>, UiState<PlpState>> { (products, facets, favoriteIds) ->
                        UiState.Success(
                            PlpState(
                                products = products,
                                availableFacets = facets,
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
    val languageCode: String,
    val query: String,
    val category: String,
    val filters: FilterState,
    val sortOption: SortOption,
)

private data class BaseProductQuery(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
    val languageCode: String,
    val query: String,
    val category: String,
    val filters: FilterState,
)

private data class SessionProductQuery(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
    val languageCode: String,
    val query: String,
    val category: String,
    val filters: FilterState,
)

private data class PlpSessionState(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
    val languageCode: String,
)

data class PlpState(
    val products: List<Product>,
    val availableFacets: List<CatalogFacet> = emptyList(),
    val favoriteIds: Set<String>,
    val filters: FilterState,
    val sortOption: SortOption,
    val canLoadMore: Boolean = true
)

private fun Product.matchesPlpQuery(query: String): Boolean {
    val normalizedQuery = query.normalizeSearchText()
    if (normalizedQuery.isBlank()) return true

    val searchableValues = buildList {
        add(title)
        add(id)
        attributesMap.forEach { (key, values) ->
            add(key)
            addAll(values)
        }
    }.filter { it.isNotBlank() }

    val searchableText = searchableValues.joinToString(" ") { it.normalizeSearchText() }
    val normalizedTokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

    return searchableText.contains(normalizedQuery) ||
        normalizedTokens.all { token -> searchableText.contains(token) }
}

private fun String.normalizeSearchText(): String {
    return Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
}
