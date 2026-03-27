package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.HomeProductsWebService
import com.example.grifon.data.catalog.toDomainProduct
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.grifon.R
import com.example.grifon.BuildConfig

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActiveShopUseCase: GetActiveShopUseCase,
    private val getCategoryTreeUseCase: GetCategoryTreeUseCase,
    private val homeProductsWebService: HomeProductsWebService,
    private val catalogApi: CatalogApi,
    private val shopPreferences: ShopPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeState>> = _uiState

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
        observeActiveShop()
    }

    private fun observeActiveShop() {
        viewModelScope.launch {
            getActiveShopUseCase().distinctUntilChanged().collect { shopId ->
                currentShopId = ShopConfig.normalizeShopId(shopId)
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
                val canViewPrices = customerId != null && shopPreferences.canViewPrices.first()
                val featured = runCatching { 
                    catalogApi.getCategoryProducts(categoryId = 2, shopId = currentShopId.toInt(), pageSize = 10, customerId = customerId).items.map { 
                        it.toDomainProduct(gatewayBaseUrl, "Featured", showPrice = canViewPrices) 
                    }
                }.getOrDefault(emptyList())

                // Φορτώνουμε όλα τα προϊόντα
                val allProducts = homeProductsWebService.fetchProductsForShop(currentShopId)
                
                val apiCategories = runCatching { getCategoryTreeUseCase(currentShopId).first() }.getOrDefault(emptyList())

                _uiState.value = UiState.Success(
                    HomeState(
                        shopId = currentShopId,
                        categories = apiCategories,
                        featuredProducts = featured,
                        allProducts = allProducts
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
                        allProducts = products
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Σφάλμα κατηγορίας: ${e.message}")
            }
        }
    }

    private suspend fun getCategoryProducts(categoryId: String?): List<Product> {
        val shopId = currentShopId.toInt()
        val customerId = shopPreferences.currentCustomerId.first()
        val canViewPrices = customerId != null && shopPreferences.canViewPrices.first()
        val response = if (categoryId.isNullOrBlank()) {
            catalogApi.getProducts(shopId = shopId, pageSize = 50, customerId = customerId)
        } else {
            catalogApi.getCategoryProducts(
                categoryId = categoryId.toInt(),
                shopId = shopId,
                pageSize = 50,
                customerId = customerId,
            )
        }
        return response.items.map { it.toDomainProduct(gatewayBaseUrl, "Grifon", showPrice = canViewPrices) }
    }
}

data class HomeState(
    val shopId: String,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val featuredProducts: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList()
)

data class CategoryIconItem(val resId: Int, val categoryId: String?)
