package com.example.grifon.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCartUseCase
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase
import com.example.grifon.domain.usecase.SyncCatalogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

@HiltViewModel
class AppViewModel @Inject constructor(
    private val getActiveShopUseCase: GetActiveShopUseCase,
    private val getCartUseCase: GetCartUseCase,
    private val getCategoryTreeUseCase: GetCategoryTreeUseCase,
    private val syncCatalogUseCase: SyncCatalogUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state
    
    private var syncJob: Job? = null

    init {
        Log.d("CrashLog", "AppViewModel: Initializing init block")
        observeAppState()
    }

    private fun observeAppState() {
        Log.d("CrashLog", "AppViewModel: Starting observeAppState")
        getActiveShopUseCase()
            .onEach { shopId ->
                Log.d("CrashLog", "AppViewModel: New shopId detected: $shopId")
                syncJob?.cancel()
                syncJob = viewModelScope.launch {
                    delay(1000)
                    Log.d("CrashLog", "AppViewModel: Triggering syncCatalogUseCase")
                    syncCatalogUseCase(shopId)
                }
            }
            .flatMapLatest { shopId ->
                combine(
                    getCartUseCase(shopId),
                    getCategoryTreeUseCase(shopId)
                ) { cartItems, categories ->
                    Log.d("CrashLog", "AppViewModel: Combining data for UI. Categories: ${categories.size}")
                    AppState(
                        activeShopId = shopId,
                        cartCount = cartItems.sumOf { it.qty },
                        categories = categories
                    )
                }
            }
            .onEach { appState -> _state.value = appState }
            .catch { e -> Log.e("CrashLog", "AppViewModel: FATAL ERROR in Flow", e) }
            .launchIn(viewModelScope)
    }
}

data class AppState(
    val activeShopId: String = "4",
    val cartCount: Int = 0,
    val categories: List<Category> = emptyList()
)
