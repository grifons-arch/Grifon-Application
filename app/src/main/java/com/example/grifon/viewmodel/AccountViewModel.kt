package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiState
import com.example.grifon.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<AccountState>>(UiState.Loading)
    val uiState: StateFlow<UiState<AccountState>> = _uiState

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    init {
        userRepository.isLoggedIn()
            .onEach { loggedIn ->
                _uiState.value = UiState.Success(AccountState(loggedIn))
            }
            .launchIn(viewModelScope)
    }

    fun onEmailChange(newValue: String) {
        _email.value = newValue
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
    }

    fun login() {
        viewModelScope.launch {
            _loginError.value = null
            // Εδώ θα καλούσατε το πραγματικό API μέσω του repository
            // Για τώρα προσομοιώνουμε το login αν τα πεδία δεν είναι κενά
            if (_email.value.isNotBlank() && _password.value.length >= 6) {
                // Πραγματική κλήση API θα πήγαινε εδώ
            } else {
                _loginError.value = "Παρακαλώ συμπληρώστε σωστά τα στοιχεία σας (Κωδικός τουλάχιστον 6 χαρακτήρες)"
            }
        }
    }
}

data class AccountState(
    val loggedIn: Boolean,
)
