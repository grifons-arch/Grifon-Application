package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.HomeProductsWebService
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.grifon.R

data class CategoryIconItem(val label: String, val resId: Int, val categoryId: String?)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActiveShopUseCase: GetActiveShopUseCase,
    private val getCategoryTreeUseCase: GetCategoryTreeUseCase,
    private val homeProductsWebService: HomeProductsWebService,
    private val catalogApi: CatalogApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeState>> = _uiState

    private var currentShopId: String = "4"

    // ΑΝΤΙΣΤΟΙΧΙΣΗ ΜΕ RES ΦΩΤΟΓΡΑΦΙΕΣ ΒΑΣΕΙ JSON IDs
    val staticCategoryIcons = listOf(
        CategoryIconItem("Όλα", R.drawable.logo, null),
        CategoryIconItem("Κεραμικά", R.drawable.kersmiks_diskodmhtiks, "4000"),
        CategoryIconItem("Αγαλματίδια", R.drawable.veroza, "4500"),
        CategoryIconItem("Διακοσμητικά", R.drawable.fvthsthka, "5000"),
        CategoryIconItem("Για χρήση", R.drawable.sapounia, "7500"),
        CategoryIconItem("Χόμπι", R.drawable.skakitabli, "7000"),
        CategoryIconItem("Αξεσουάρ", R.drawable.yfasmatina, "8000")
    )

    init {
        observeActiveShop()
    }

    private fun observeActiveShop() {
        viewModelScope.launch {
            getActiveShopUseCase().distinctUntilChanged().collect { shopId ->
                currentShopId = if (shopId == "shop_a" || shopId == "1") "1" else "4"
                loadInitialData()
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            val products = runCatching { 
                homeProductsWebService.fetchProductsForShop(currentShopId) 
            }.getOrDefault(emptyList())

            val apiCategories = runCatching { 
                getCategoryTreeUseCase(currentShopId).first() 
            }.getOrDefault(emptyList())

            _uiState.value = UiState.Success(
                HomeState(
                    shopId = currentShopId,
                    categories = apiCategories,
                    products = products
                )
            )
        }
    }

    fun selectCategory(categoryId: String?) {
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val products = runCatching { 
                getCategoryProducts(categoryId) 
            }.getOrDefault(emptyList())
            
            _uiState.value = UiState.Success(
                currentState.copy(
                    selectedCategoryId = categoryId,
                    products = products
                )
            )
        }
    }

    private suspend fun getCategoryProducts(categoryId: String?): List<Product> {
        val response = if (categoryId == null || categoryId == "2") {
            catalogApi.getProducts(shopId = currentShopId.toInt(), pageSize = 100)
        } else {
            catalogApi.getCategoryProducts(categoryId = categoryId.toInt(), shopId = currentShopId.toInt())
        }
        
        return response.items.map { dto ->
            Product(
                id = dto.id.toString(),
                title = dto.name ?: "",
                price = dto.price ?: 0.0,
                currency = "EUR",
                imageUrl = if (dto.defaultImage?.url?.startsWith("/") == true) 
                    "http://10.0.2.2:3000${dto.defaultImage.url}" else dto.defaultImage?.url ?: "",
                brand = "Grifon",
                rating = 0.0,
                inStock = true,
                attributesMap = mapOf("reference" to (dto.reference ?: ""))
            )
        }
    }
}

data class HomeState(
    val shopId: String,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val products: List<Product> = emptyList()
)
