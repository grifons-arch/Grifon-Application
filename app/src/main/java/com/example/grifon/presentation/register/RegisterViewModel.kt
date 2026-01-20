package com.example.grifon.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.domain.auth.RegisterOutcome
import com.example.grifon.domain.auth.RegisterParams
import com.example.grifon.domain.auth.RegisterUseCase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onConfirmEmailChange(value: String) {
        _uiState.update { it.copy(confirmEmail = value) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value) }
    }

    fun onCompanyNameChange(value: String) {
        _uiState.update { it.copy(companyName = value) }
    }

    fun onVatNumberChange(value: String) {
        _uiState.update { it.copy(vatNumber = value) }
    }

    fun onTaxOfficeChange(value: String) {
        _uiState.update { it.copy(taxOffice = value) }
    }

    fun onAddressChange(value: String) {
        _uiState.update { it.copy(address = value) }
    }

    fun onCityChange(value: String) {
        _uiState.update { it.copy(city = value) }
    }

    fun onCountryChange(name: String, iso: String) {
        _uiState.update {
            it.copy(
                countryName = name,
                countryIso = iso,
                city = "",
                address = "",
            )
        }
    }

    fun onPostalCodeChange(value: String) {
        _uiState.update { it.copy(postalCode = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onAcceptTermsChange(value: Boolean) {
        _uiState.update { it.copy(acceptTerms = value) }
    }

    fun onSubscribeNewsletterChange(value: Boolean) {
        _uiState.update { it.copy(subscribeNewsletter = value) }
    }

    fun onSubmit() {
        val currentState = _uiState.value
        if (!currentState.isSubmitEnabled) return

        _uiState.update { it.copy(status = RegisterStatus.Loading) }
        viewModelScope.launch {
            val result = registerUseCase(
                RegisterParams(
                    email = currentState.email.trim(),
                    password = currentState.password,
                    firstName = currentState.firstName.trim(),
                    lastName = currentState.lastName.trim(),
                    countryIso = currentState.countryIso.trim().uppercase(),
                    phone = currentState.phone.trim().ifBlank { null },
                    company = currentState.companyName.trim().ifBlank { null },
                ),
            )
            _uiState.update {
                when (result) {
                    is RegisterOutcome.Success -> it.copy(
                        status = RegisterStatus.Success(result.result.message),
                    )
                    is RegisterOutcome.Error -> it.copy(
                        status = RegisterStatus.Error(result.message),
                    )
                }
            }
        }
    }

    fun onGoogleAccountReceived(account: GoogleSignInAccount) {
        val displayName = account.displayName ?: ""
        val (firstName, lastName) = parseNameParts(
            account.givenName,
            account.familyName,
            displayName,
        )
        val email = account.email.orEmpty()
        _uiState.update {
            it.copy(
                googleDisplayName = displayName.ifBlank { null },
                googleAccountEmail = email.ifBlank { null },
                googleSignInError = null,
                firstName = firstName.ifBlank { it.firstName },
                lastName = lastName.ifBlank { it.lastName },
                email = if (email.isNotBlank()) email else it.email,
            )
        }
    }

    fun onGoogleAccountError(message: String) {
        _uiState.update { it.copy(googleSignInError = message) }
    }

    private fun parseNameParts(
        givenName: String?,
        familyName: String?,
        displayName: String,
    ): Pair<String, String> {
        if (!givenName.isNullOrBlank() || !familyName.isNullOrBlank()) {
            return givenName.orEmpty() to familyName.orEmpty()
        }
        val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "" to ""
            parts.size == 1 -> parts.first() to ""
            else -> parts.first() to parts.drop(1).joinToString(" ")
        }
    }
}
