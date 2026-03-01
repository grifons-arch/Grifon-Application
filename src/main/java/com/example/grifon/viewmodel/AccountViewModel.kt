package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<AccountState>>(UiState.Loading)
    val uiState: StateFlow<UiState<AccountState>> = _uiState

    init {
        userRepository.isLoggedIn()
            .onEach { loggedIn ->
                val currentError = (_uiState.value as? UiState.Success)?.data?.loginError
                _uiState.value = UiState.Success(AccountState(loggedIn, currentError))
            }
            .launchIn(viewModelScope)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val currentState = (_uiState.value as? UiState.Success)?.data
            val result = userRepository.login(email, password)
            
            if (result.isFailure) {
                _uiState.value = UiState.Success(
                    AccountState(
                        loggedIn = currentState?.loggedIn ?: false,
                        loginError = result.exceptionOrNull()?.message ?: "Σφάλμα σύνδεσης"
                    )
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    fun clearError() {
        val currentState = (_uiState.value as? UiState.Success)?.data
        if (currentState != null) {
            _uiState.value = UiState.Success(currentState.copy(loginError = null))
        }
    }
}

data class AccountState(
    val loggedIn: Boolean,
    val loginError: String? = null
)
