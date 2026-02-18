package com.example.grifon.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.domain.auth.RegisterOutcome
import com.example.grifon.domain.auth.RegisterParams
import com.example.grifon.domain.auth.RegisterUseCase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun onSocialTitleChange(value: String) {
        _uiState.update { it.copy(socialTitle = value) }
    }

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value) }
    }

    fun onIbanChange(value: String) {
        _uiState.update { it.copy(iban = value) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onEmailConfirmationChange(value: String) {
        _uiState.update { it.copy(emailConfirmation = value) }
    }

    fun onCompanyNameChange(value: String) {
        _uiState.update { it.copy(companyName = value) }
    }

    fun onVatNumberChange(value: String) {
        _uiState.update { it.copy(vatNumber = value) }
    }

    fun onCountryChange(value: String) {
        _uiState.update { it.copy(country = value) }
    }

    fun onCityChange(value: String) {
        _uiState.update { it.copy(city = value) }
    }

    fun onStreetChange(value: String) {
        _uiState.update { it.copy(street = value) }
    }

    fun onPostalCodeChange(value: String) {
        _uiState.update { it.copy(postalCode = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onPasswordConfirmationChange(value: String) {
        _uiState.update { it.copy(passwordConfirmation = value) }
    }

    fun onCustomerDataPrivacyAcceptedChange(value: Boolean) {
        _uiState.update { it.copy(customerDataPrivacyAccepted = value) }
    }

    fun onNewsletterOptInChange(value: Boolean) {
        _uiState.update { it.copy(newsletterOptIn = value) }
    }

    fun onTermsAndPrivacyAcceptedChange(value: Boolean) {
        _uiState.update { it.copy(termsAndPrivacyAccepted = value) }
    }

    fun onSubmit() {
        val currentState = _uiState.value
        if (!currentState.isSubmitEnabled) return

        val countryIso = normalizeCountryIso(currentState.country)
        if (countryIso == null) {
            _uiState.update {
                it.copy(status = RegisterStatus.Error("Συμπληρώστε έγκυρο κωδικό χώρας (π.χ. GR)."))
            }
            return
        }

        _uiState.update { it.copy(status = RegisterStatus.Loading) }
        viewModelScope.launch {
            val params = RegisterParams(
                email = currentState.email.trim(),
                password = currentState.password,
                socialTitle = currentState.socialTitle.trim().ifBlank { null },
                firstName = currentState.firstName.trim(),
                lastName = currentState.lastName.trim(),
                countryIso = countryIso,
                street = currentState.street.trim(),
                city = currentState.city.trim(),
                postalCode = currentState.postalCode.trim(),
                phone = currentState.phone.trim().ifBlank { null },
                company = currentState.companyName.trim().ifBlank { null },
                vatNumber = currentState.vatNumber.trim().ifBlank { null },
                iban = currentState.iban.trim().ifBlank { null },
                customerDataPrivacyAccepted = currentState.customerDataPrivacyAccepted,
                newsletter = currentState.newsletterOptIn,
                termsAndPrivacyAccepted = currentState.termsAndPrivacyAccepted,
            )
            val result = withContext(Dispatchers.IO) {
                registerUseCase(params)
            }
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
                emailConfirmation = if (email.isNotBlank()) email else it.emailConfirmation,
            )
        }
    }

    fun onGoogleAccountError(message: String) {
        _uiState.update { it.copy(googleSignInError = message) }
    }

    private fun normalizeCountryIso(rawCountry: String): String? {
        val country = rawCountry.trim()
        if (country.isBlank()) return null

        if (country.length == 2) {
            return country.uppercase()
        }

        return when (country.lowercase()) {
            "ελλάδα", "ελλαδα", "greece", "ellada", "hellas" -> "GR"
            else -> null
        }
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
