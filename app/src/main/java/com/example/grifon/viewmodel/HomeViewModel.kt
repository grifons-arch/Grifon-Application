package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.catalog.HomeProductsWebService
import com.example.grifon.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeProductsWebService: HomeProductsWebService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeState>> = _uiState

    init {
        loadAllProducts()
    }

    fun loadAllProducts() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                // Φέρνουμε προϊόντα από το Shop 4 (GR) και Shop 1 (SE)
                val products = coroutineScope {
                    val shopGrDeferred = async { 
                        runCatching { homeProductsWebService.fetchProductsForShop("4") }.getOrDefault(emptyList()) 
                    }
                    val shopSeDeferred = async { 
                        runCatching { homeProductsWebService.fetchProductsForShop("1") }.getOrDefault(emptyList()) 
                    }
                    
                    shopGrDeferred.await() + shopSeDeferred.await()
                }

                if (products.isEmpty()) {
                    _uiState.value = UiState.Error("Δεν βρέθηκαν προϊόντα στα καταστήματα.")
                } else {
                    _uiState.value = UiState.Success(
                        HomeState(
                            shopId = "all",
                            banners = listOf("Νέες Παραλαβές", "Προσφορές Grifon"),
                            popular = products.shuffled().take(20),
                            recent = products.sortedByDescending { it.id }.take(20),
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Σφάλμα κατά τη φόρτωση των προϊόντων")
            }
        }
    }
}

data class HomeState(
    val shopId: String,
    val banners: List<String>,
    val popular: List<Product>,
    val recent: List<Product>,
)
