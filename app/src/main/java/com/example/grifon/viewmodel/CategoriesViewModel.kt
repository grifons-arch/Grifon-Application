package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    getActiveShopUseCase: GetActiveShopUseCase,
    getCategoryTreeUseCase: GetCategoryTreeUseCase,
) : ViewModel() {
    private val _expanded = MutableStateFlow(setOf<String>())
    private val _uiState = MutableStateFlow<UiState<CategoriesState>>(UiState.Loading)
    val uiState: StateFlow<UiState<CategoriesState>> = _uiState

    init {
        combine(getActiveShopUseCase(), _expanded) { shopId, expanded ->
            shopId to expanded
        }.onEach { (shopId, expanded) ->
            getCategoryTreeUseCase(shopId)
                .onEach { categories ->
                    _uiState.value = UiState.Success(CategoriesState(categories, expanded))
                }
                .launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }

    fun toggle(categoryId: String) {
        _expanded.value = if (_expanded.value.contains(categoryId)) {
            _expanded.value - categoryId
        } else {
            _expanded.value + categoryId
        }
    }
}

data class CategoriesState(
    val categories: List<Category>,
    val expanded: Set<String>,
)
