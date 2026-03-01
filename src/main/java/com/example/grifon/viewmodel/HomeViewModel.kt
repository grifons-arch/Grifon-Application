package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.HomeProductsWebService
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActiveShopUseCase: GetActiveShopUseCase,
    private val homeProductsWebService: HomeProductsWebService,
    private val catalogApi: CatalogApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeState>> = _uiState

    private var currentShopId: String = "4"
    
    // Προεπιλεγμένες κατηγορίες σε περίπτωση σφάλματος του API
    private val defaultCategories = listOf(
        Category("3", "Κεραμικά", null, 0),
        Category("4", "Αγαλματίδια κ.α.", null, 0),
        Category("5", "Διακοσμητικά", null, 0),
        Category("6", "Για χρήση", null, 0),
        Category("7", "Χόμπι και παιχνίδια", null, 0),
        Category("8", "Αξεσουάρ", null, 0)
    )

    init {
        observeActiveShop()
    }

    private fun observeActiveShop() {
        viewModelScope.launch {
            getActiveShopUseCase()
                .distinctUntilChanged()
                .collect { shopId ->
                    currentShopId = if (shopId == "shop_a" || shopId == "1") "1" else "4"
                    loadInitialData()
                }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val products = runCatching { 
                    homeProductsWebService.fetchProductsForShop(currentShopId) 
                }.getOrDefault(emptyList())

                val categoriesResponse = try {
                    val resp = catalogApi.getCategories(shopId = currentShopId.toInt())
                    if (resp.items.isEmpty()) defaultCategories else resp.items.map { 
                        Category(id = it.id.toString(), name = it.name ?: "", parentId = null, childrenCount = 0)
                    }
                } catch (e: Exception) {
                    defaultCategories
                }

                _uiState.value = UiState.Success(
                    HomeState(
                        shopId = currentShopId,
                        categories = categoriesResponse,
                        selectedCategoryId = null,
                        popular = products,
                        recent = products.take(10)
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Σφάλμα σύνδεσης. Βεβαιωθείτε ότι ο Gateway τρέχει.")
            }
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
                    val response = catalogApi.getCategoryProducts(categoryId = categoryId.toInt(), shopId = currentShopId.toInt())
                    response.items.map { dto ->
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
                _uiState.value = UiState.Success(currentState.copy(selectedCategoryId = categoryId, popular = products))
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Σφάλμα κατά τη φόρτωση της κατηγορίας.")
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
