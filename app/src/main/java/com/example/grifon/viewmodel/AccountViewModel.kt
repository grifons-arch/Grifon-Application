package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.repository.UserRepository
import com.example.grifon.domain.model.User
import com.example.grifon.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<AccountState>>(UiState.Loading)
    val uiState: StateFlow<UiState<AccountState>> = _uiState

    init {
        observeLoginState()
    }

    private fun observeLoginState() {
        combine(
            userRepository.isLoggedIn(),
            userRepository.getUserName(),
            userRepository.getUserDetails()
        ) { loggedIn, name, details ->
            AccountState(
                loggedIn = loggedIn, 
                userName = name,
                userDetails = details
            )
        }
        .onEach { state ->
            _uiState.value = UiState.Success(state)
        }
        .launchIn(viewModelScope)
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            updateError("Παρακαλώ συμπληρώστε όλα τα πεδία")
            return
        }
        
        viewModelScope.launch {
            val currentState = (_uiState.value as? UiState.Success)?.data ?: AccountState(false)
            _uiState.value = UiState.Success(currentState.copy(isLoading = true, loginError = null))
            
            val success = loginUseCase(email, pass)
            
            if (!success) {
                updateError("Λανθασμένα στοιχεία σύνδεσης. Δοκιμάστε ξανά.")
            }
        }
    }

    fun updateProfile(user: User) {
        viewModelScope.launch {
            val currentState = (_uiState.value as? UiState.Success)?.data ?: return@launch
            _uiState.value = UiState.Success(currentState.copy(isLoading = true))
            
            val success = userRepository.updateProfile(user)
            if (!success) {
                updateError("Η ενημέρωση απέτυχε. Δοκιμάστε ξανά.")
            }
            // Η επιτυχία ενημερώνει το flow και άρα το UI αυτόματα
        }
    }

    private fun updateError(message: String) {
        val currentState = (_uiState.value as? UiState.Success)?.data ?: AccountState(false)
        _uiState.value = UiState.Success(currentState.copy(loginError = message, isLoading = false))
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }
}

data class AccountState(
    val loggedIn: Boolean,
    val userName: String? = null,
    val userDetails: User? = null,
    val loginError: String? = null,
    val isLoading: Boolean = false
)
