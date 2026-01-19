package com.example.grifon

import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class RegisterViewModel : ViewModel() {
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
        // TODO: Hook into registration API.
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
