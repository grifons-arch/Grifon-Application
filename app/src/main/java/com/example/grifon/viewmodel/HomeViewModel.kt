package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.HomeProductsWebService
import com.example.grifon.data.catalog.toDomainProduct
import com.example.grifon.data.local.LocalPriceAccessService
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.FavoriteProduct
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.RecentProduct
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase
import com.example.grifon.domain.usecase.ObserveFavoritesUseCase
import com.example.grifon.domain.usecase.ObserveRecentProductsUseCase
import com.example.grifon.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.grifon.R
import com.example.grifon.BuildConfig
import com.example.grifon.core.PrestaLanguage

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActiveShopUseCase: GetActiveShopUseCase,
    private val getCategoryTreeUseCase: GetCategoryTreeUseCase,
    private val observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val observeRecentProductsUseCase: ObserveRecentProductsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val homeProductsWebService: HomeProductsWebService,
    private val catalogApi: CatalogApi,
    private val shopPreferences: ShopPreferences,
    private val localPriceAccessService: LocalPriceAccessService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeState>> = _uiState
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())

    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")
    private var currentShopId: String = "4"

    val staticCategoryIcons = listOf(
        CategoryIconItem(R.drawable.logo, null),
        CategoryIconItem(R.drawable.kersmiks_diskodmhtiks, "4000"),
        CategoryIconItem(R.drawable.veroza, "4500"),
        CategoryIconItem(R.drawable.fvthsthka, "5000"),
        CategoryIconItem(R.drawable.sapounia, "7500"),
        CategoryIconItem(R.drawable.skakitabli, "7000"),
        CategoryIconItem(R.drawable.yfasmatina, "8000")
    )

    init {
        observeFavorites()
        observeActiveShop()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            getActiveShopUseCase()
                .map { ShopConfig.normalizeShopId(it) }
                .distinctUntilChanged()
                .flatMapLatest { shopId ->
                    observeFavoritesUseCase(shopId)
                }
                .map { favorites -> favorites.map { it.productId }.toSet() }
                .collect { favoriteIds ->
                    _favoriteIds.value = favoriteIds
                    val currentState = (_uiState.value as? UiState.Success)?.data ?: return@collect
                    _uiState.value = UiState.Success(currentState.copy(favoriteIds = favoriteIds))
                }
        }
    }

    private fun observeActiveShop() {
        viewModelScope.launch {
            combine(
                getActiveShopUseCase(),
                shopPreferences.currentCustomerId,
                shopPreferences.canViewPrices,
                shopPreferences.appLanguage,
            ) { shopId, customerId, canViewPrices, languageCode ->
                HomeSessionState(ShopConfig.normalizeShopId(shopId), customerId, canViewPrices, languageCode)
            }.flatMapLatest { session ->
                localPriceAccessService.observeCanDisplayPrices(
                    shopId = session.shopId,
                    customerId = session.customerId,
                    canViewPrices = session.canViewPrices,
                ).map { canDisplayPrices -> session.shopId to canDisplayPrices to session.languageCode }
            }
                .distinctUntilChanged()
                .collect { (shopState, languageCode) ->
                    val (shopId, _) = shopState
                    currentShopId = shopId
                    loadInitialData()
                }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Φορτώνουμε τα προτεινόμενα (π.χ. από κατηγορία 2)
                val customerId = shopPreferences.currentCustomerId.first()
                val canDisplayPrices = localPriceAccessService.canDisplayPrices(
                    shopId = currentShopId,
                    customerId = customerId,
                    canViewPrices = shopPreferences.canViewPrices.first(),
                )
                // Φορτώνουμε όλα τα προϊόντα
                val allProducts = homeProductsWebService.fetchProductsForShop(currentShopId)
                val featured = buildFeaturedProducts(
                    shopId = currentShopId,
                    allProducts = allProducts,
                    canViewPrices = canDisplayPrices,
                )
                
                val apiCategories = runCatching { getCategoryTreeUseCase(currentShopId).first() }.getOrDefault(emptyList())

                _uiState.value = UiState.Success(
                    HomeState(
                        shopId = currentShopId,
                        categories = apiCategories,
                        featuredProducts = featured,
                        allProducts = allProducts,
                        favoriteIds = _favoriteIds.value,
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Αποτυχία σύνδεσης: ${e.message}")
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val products = getCategoryProducts(categoryId)
                _uiState.value = UiState.Success(
                    currentState.copy(
                        selectedCategoryId = categoryId,
                        allProducts = products,
                        favoriteIds = _favoriteIds.value,
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Σφάλμα κατηγορίας: ${e.message}")
            }
        }
    }

    private suspend fun getCategoryProducts(categoryId: String?): List<Product> {
        val shopId = currentShopId.toInt()
        val langId = PrestaLanguage.toLangId(shopPreferences.appLanguage.first())
        val customerId = shopPreferences.currentCustomerId.first()
        val canDisplayPrices = localPriceAccessService.canDisplayPrices(
            shopId = currentShopId,
            customerId = customerId,
            canViewPrices = shopPreferences.canViewPrices.first(),
        )
        val requestCustomerId = customerId?.takeIf { canDisplayPrices }
        val response = if (categoryId.isNullOrBlank()) {
            catalogApi.getProducts(
                shopId = shopId,
                lang = langId,
                pageSize = 50,
                customerId = requestCustomerId
            )
        } else {
            catalogApi.getCategoryProducts(
                categoryId = categoryId.toInt(),
                shopId = shopId,
                lang = langId,
                pageSize = 50,
                customerId = requestCustomerId,
            )
        }
        return response.items.map { it.toDomainProduct(gatewayBaseUrl, "Grifon", showPrice = canDisplayPrices) }
    }

    fun toggleFavorite(product: Product) {
        viewModelScope.launch {
            toggleFavoriteUseCase(currentShopId, product)
        }
    }

    private suspend fun buildFeaturedProducts(
        shopId: String,
        allProducts: List<Product>,
        canViewPrices: Boolean,
    ): List<Product> {
        val favorites = observeFavoritesUseCase(shopId).first()
        if (favorites.isNotEmpty()) {
            return favorites.take(10).map { it.toProduct(canViewPrices) }
        }

        val recentProducts = observeRecentProductsUseCase(shopId, limit = 10).first()
        if (recentProducts.isNotEmpty()) {
            return recentProducts.map { it.toProduct(canViewPrices) }
        }

        return allProducts.shuffled().take(10)
    }
}

private fun FavoriteProduct.toProduct(canViewPrices: Boolean): Product {
    return Product(
        id = productId,
        title = title,
        price = price?.takeIf { canViewPrices },
        currency = currency,
        imageUrl = imageUrl,
        images = emptyList(),
        brand = brand,
        rating = 0.0,
        inStock = true,
        attributesMap = emptyMap(),
    )
}

private fun RecentProduct.toProduct(canViewPrices: Boolean): Product {
    return Product(
        id = productId,
        title = title,
        price = price?.takeIf { canViewPrices },
        currency = currency,
        imageUrl = imageUrl,
        images = emptyList(),
        brand = brand,
        rating = 0.0,
        inStock = true,
        attributesMap = emptyMap(),
    )
}

data class HomeState(
    val shopId: String,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val featuredProducts: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
)

data class CategoryIconItem(val resId: Int, val categoryId: String?)

private data class HomeSessionState(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
    val languageCode: String,
)
