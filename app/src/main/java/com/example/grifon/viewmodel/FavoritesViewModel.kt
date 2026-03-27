package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.ShopConfig
import com.example.grifon.core.UiState
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.FavoriteProduct
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.ObserveFavoritesUseCase
import com.example.grifon.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    shopPreferences: ShopPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<FavoritesState>>(UiState.Loading)
    val uiState: StateFlow<UiState<FavoritesState>> = _uiState
    private var currentShopId: String = ShopConfig.GreekShopId

    init {
        combine(
            getActiveShopUseCase(),
            shopPreferences.currentCustomerId,
        ) { shopId, customerId ->
            ShopConfig.normalizeShopId(shopId) to customerId
        }
            .distinctUntilChanged()
            .flatMapLatest { (shopId, customerId) ->
                currentShopId = shopId
                observeFavoritesUseCase(shopId).onEach { favorites ->
                    _uiState.value = UiState.Success(
                        FavoritesState(
                            isLoggedIn = customerId != null,
                            favorites = favorites,
                        )
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun toggleFavorite(product: FavoriteProduct) {
        viewModelScope.launch {
            toggleFavoriteUseCase(currentShopId, product.toProduct())
        }
    }
}

data class FavoritesState(
    val isLoggedIn: Boolean,
    val favorites: List<FavoriteProduct>,
)

private fun FavoriteProduct.toProduct(): Product {
    return Product(
        id = productId,
        title = title,
        price = price,
        currency = currency,
        imageUrl = imageUrl,
        images = emptyList(),
        brand = brand,
        rating = 0.0,
        inStock = true,
        attributesMap = emptyMap(),
    )
}
