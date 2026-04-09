package com.example.grifon.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.LoginText
import com.example.grifon.core.UiState
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.data.repository.UserRepository
import com.example.grifon.core.loginText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val shopPreferences: ShopPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<AccountState>>(UiState.Loading)
    val uiState: StateFlow<UiState<AccountState>> = _uiState

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn

    init {
        combine(
            userRepository.isLoggedIn(),
            shopPreferences.canViewPrices,
        ) { loggedIn, canViewPrices ->
            AccountState(
                loggedIn = loggedIn,
                canViewPrices = canViewPrices,
            )
        }
            .onEach { state ->
                _uiState.value = UiState.Success(state)
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

    fun onEmailChange(newValue: String) {
        _email.value = newValue
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
    }

    fun login() {
        if (_email.value.isBlank() || _password.value.length < 6) {
            _loginError.value = loginText(context, LoginText.LoginValidationError)
            return
        }

        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null
            
            userRepository.login(_email.value, _password.value)
                .onSuccess {
                    // Η κατάσταση loggedIn θα ενημερωθεί αυτόματα μέσω του init block
                    _isLoggingIn.value = false
                }
                .onFailure { error ->
                    _loginError.value = error.message ?: loginText(context, LoginText.LoginFailed)
                    _isLoggingIn.value = false
                }
        }
    }

    fun logout() {
        userRepository.logout()
    }
}

data class AccountState(
    val loggedIn: Boolean,
    val canViewPrices: Boolean,
)
