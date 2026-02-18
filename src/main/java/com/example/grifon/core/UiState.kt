package com.example.grifon.core

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
}
