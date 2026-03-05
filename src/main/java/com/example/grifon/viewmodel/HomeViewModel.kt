package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.HomeProductsWebService
import com.example.grifon.data.local.UserPreferences
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActiveShopUseCase: GetActiveShopUseCase,
    private val homeProductsWebService: HomeProductsWebService,
    private val catalogApi: CatalogApi,
    private val userPreferences: UserPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeState>> = _uiState

    private var currentShopId: String = "4"
    
    private val defaultCategories = listOf(
        Category("4000", "Κεραμικά", null, 0),
        Category("4500", "Αγαλματίδια", null, 0),
        Category("5000", "Διακοσμητικά", null, 0),
        Category("7500", "Για χρήση", null, 0),
        Category("7000", "Χόμπι", null, 0),
        Category("8000", "Αξεσουάρ", null, 0)
    )

    init {
        observeActiveShop()
    }

    private fun observeActiveShop() {
        viewModelScope.launch {
            getActiveShopUseCase()
                .distinctUntilChanged()
                .collect { shopId ->
                    currentShopId = if (shopId == "1") "1" else "4"
                    loadInitialData()
                }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val categoriesResponse = try {
                    val resp = catalogApi.getCategories(shopId = currentShopId.toInt())
                    if (resp.items.isEmpty()) defaultCategories else resp.items.map {
                        Category(id = it.id.toString(), name = it.name ?: "", parentId = null, childrenCount = 0)
                    }
                } catch (e: Exception) {
                    defaultCategories
                }

                val initialCategoryId = "4000"
                val products = try {
                    fetchProductsForCategory(initialCategoryId)
                } catch (e: Exception) {
                    // Fallback αν αποτύχει η κατηγορία
                    homeProductsWebService.fetchProductsForShop(currentShopId)
                }

                _uiState.value = UiState.Success(
                    HomeState(
                        shopId = currentShopId,
                        categories = categoriesResponse,
                        selectedCategoryId = initialCategoryId,
                        popular = products,
                        recent = products.take(10)
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Σφάλμα σύνδεσης.")
            }
        }
    }

    private suspend fun fetchProductsForCategory(categoryId: String): List<Product> {
        val cId = userPreferences.customerId.firstOrNull()?.toIntOrNull()
        val response = catalogApi.getCategoryProducts(
            categoryId = categoryId.toInt(), 
            shopId = currentShopId.toInt(),
            customerId = cId,
            pageSize = 50
        )
        return response.items.map { dto ->
            val gatewayBaseUrl = "http://10.0.2.2:3000"
            val rawUrl = dto.defaultImage?.url ?: ""
            val fullImageUrl = if (rawUrl.startsWith("/")) "$gatewayBaseUrl$rawUrl" else rawUrl

            Product(
                id = dto.id.toString(),
                title = dto.name ?: "",
                price = dto.price ?: 0.0,
                currency = "EUR",
                imageUrl = fullImageUrl,
                brand = dto.brand ?: "Grifon",
                rating = 0.0,
                inStock = dto.inStock ?: true,
                attributesMap = mapOf("reference" to (dto.reference ?: ""))
            )
        }
    }

    fun selectCategory(categoryId: String?) {
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val products = if (categoryId == null) {
                    homeProductsWebService.fetchProductsForShop(currentShopId)
                } else {
                    fetchProductsForCategory(categoryId)
                }
                _uiState.value = UiState.Success(currentState.copy(selectedCategoryId = categoryId, popular = products))
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Σφάλμα φόρτωσης κατηγορίας.")
            }
        }
    }
}

data class HomeState(
    val shopId: String,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val popular: List<Product>,
    val recent: List<Product>,
)
