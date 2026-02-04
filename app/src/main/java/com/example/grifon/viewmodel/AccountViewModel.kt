package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import com.example.grifon.core.UiState
import com.example.grifon.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import androidx.lifecycle.viewModelScope

@HiltViewModel
class AccountViewModel @Inject constructor(
    userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<AccountState>>(UiState.Loading)
    val uiState: StateFlow<UiState<AccountState>> = _uiState

    init {
        userRepository.isLoggedIn()
            .onEach { loggedIn ->
                _uiState.value = UiState.Success(AccountState(loggedIn))
            }
            .launchIn(viewModelScope)
    }
}

data class AccountState(
    val loggedIn: Boolean,
)
