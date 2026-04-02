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
import java.util.Locale

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

    fun onContactPersonFullNameChange(value: String) {
        _uiState.update { it.copy(contactPersonFullName = value) }
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

    fun onAddressCoordinatesChange(value: String) {
        _uiState.update { it.copy(addressCoordinates = value) }
    }

    fun onCompanyRegistrationFileSelected(value: String?) {
        _uiState.update { it.copy(companyRegistrationFileName = value) }
    }

    fun onInvoiceFileSelected(value: String?) {
        _uiState.update { it.copy(invoiceFileName = value) }
    }

    fun onCountryChange(value: String) {
        val resolvedCountry = RegisterAddressCatalog.resolveCountry(value, Locale.getDefault())
        if (resolvedCountry != null) {
            onCountrySelected(resolvedCountry)
            return
        }

        _uiState.update {
            it.copy(
                country = value,
                countryIso = "",
                city = "",
                street = "",
                postalCode = "",
            )
        }
    }

    fun onCountrySelected(country: CountryOption) {
        _uiState.update { currentState ->
            val hasChanged = currentState.countryIso != country.isoCode
            currentState.copy(
                country = country.displayName,
                countryIso = country.isoCode,
                city = if (hasChanged) "" else currentState.city,
                street = if (hasChanged) "" else currentState.street,
                postalCode = if (hasChanged) "" else currentState.postalCode,
            )
        }
    }

    fun onCityChange(value: String) {
        _uiState.update { currentState ->
            val hasChanged = currentState.city.trim() != value.trim()
            currentState.copy(
                city = value,
                street = if (hasChanged) "" else currentState.street,
                postalCode = if (hasChanged) "" else currentState.postalCode,
            )
        }
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

    fun onWholesaleRequestedChange(value: Boolean) {
        _uiState.update { it.copy(wholesaleRequested = value) }
    }

    fun onSubmit() {
        val currentState = _uiState.value
        if (!currentState.isSubmitEnabled) return

        val countryIso = currentState.countryIso.ifBlank {
            normalizeCountryIso(currentState.country) ?: defaultCountryIso()
        }

        _uiState.update { it.copy(status = RegisterStatus.Loading) }
        viewModelScope.launch {
            val contactPersonFullName = currentState.contactPersonFullName
                .trim()
                .ifBlank { listOf(currentState.firstName, currentState.lastName).joinToString(" ").trim() }
                .takeIf { it.isNotBlank() }
            val requiresStructuredAddress = currentState.wholesaleRequested
            val resolvedStreet = if (requiresStructuredAddress) {
                currentState.street.trim()
            } else {
                currentState.street.trim().ifBlank { "Online registration" }
            }
            val resolvedCity = if (requiresStructuredAddress) {
                currentState.city.trim()
            } else {
                currentState.city.trim().ifBlank {
                    if (countryIso == "SE") "Stockholm" else "Athens"
                }
            }
            val resolvedPostalCode = if (requiresStructuredAddress) {
                currentState.postalCode.trim()
            } else {
                currentState.postalCode.trim().ifBlank {
                    if (countryIso == "SE") "11122" else "10552"
                }
            }
            val params = RegisterParams(
                email = currentState.email.trim(),
                password = currentState.password,
                socialTitle = currentState.socialTitle.trim().ifBlank { null },
                firstName = currentState.firstName.trim(),
                lastName = currentState.lastName.trim(),
                contactPersonFullName = contactPersonFullName,
                countryIso = countryIso,
                street = resolvedStreet,
                city = resolvedCity,
                postalCode = resolvedPostalCode,
                phone = currentState.phone.trim().ifBlank { null },
                company = currentState.companyName.trim().ifBlank { null },
                vatNumber = currentState.vatNumber.trim().ifBlank { null },
                addressCoordinates = currentState.addressCoordinates.trim().ifBlank { null },
                iban = currentState.iban.trim().ifBlank { null },
                companyRegistrationFileName = currentState.companyRegistrationFileName,
                invoiceFileName = currentState.invoiceFileName,
                customerDataPrivacyAccepted = currentState.customerDataPrivacyAccepted,
                newsletter = currentState.newsletterOptIn,
                termsAndPrivacyAccepted = currentState.termsAndPrivacyAccepted,
                wholesaleRequested = currentState.wholesaleRequested,
            )
            val result = registerUseCase(params)
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
        return RegisterAddressCatalog.resolveCountry(rawCountry, Locale.getDefault())?.isoCode
    }

    private fun defaultCountryIso(): String {
        val locale = Locale.getDefault()
        val region = locale.country.uppercase(Locale.ROOT)
        if (region in setOf("GR", "SE")) {
            return region
        }
        return if (locale.language.equals("sv", ignoreCase = true)) "SE" else "GR"
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
