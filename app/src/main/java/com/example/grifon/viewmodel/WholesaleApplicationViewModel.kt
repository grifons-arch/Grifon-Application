package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.auth.RegisterOutcome
import com.example.grifon.domain.auth.SubmitWholesaleApplicationUseCase
import com.example.grifon.domain.auth.WholesaleApplicationParams
import com.example.grifon.presentation.register.CountryOption
import com.example.grifon.presentation.register.RegisterAddressCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WholesaleApplicationViewModel @Inject constructor(
    private val shopPreferences: ShopPreferences,
    private val submitWholesaleApplicationUseCase: SubmitWholesaleApplicationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WholesaleApplicationUiState())
    val uiState: StateFlow<WholesaleApplicationUiState> = _uiState

    init {
        val sessionFlow = combine(
            shopPreferences.currentCustomerId,
            shopPreferences.canViewPrices,
        ) { customerId, canViewPrices ->
            customerId to canViewPrices
        }

        val profileFlow = combine(
            shopPreferences.currentCustomerEmail,
            shopPreferences.currentCustomerFirstName,
            shopPreferences.currentCustomerLastName,
            shopPreferences.currentCustomerCompany,
        ) { email, firstName, lastName, company ->
            ProfileSnapshot(
                email = email,
                firstName = firstName,
                lastName = lastName,
                company = company,
            )
        }

        combine(sessionFlow, profileFlow) { session, profile ->
            SessionProfile(
                customerId = session.first,
                email = profile.email,
                firstName = profile.firstName,
                lastName = profile.lastName,
                company = profile.company,
                canViewPrices = session.second,
            ) 
        }.onEach { session ->
            _uiState.update { current ->
                val sessionFullName = listOfNotNull(
                    session.firstName?.trim()?.takeIf { it.isNotEmpty() },
                    session.lastName?.trim()?.takeIf { it.isNotEmpty() },
                ).joinToString(" ")

                current.copy(
                    customerId = session.customerId,
                    isEligible = session.customerId != null && !session.canViewPrices,
                    email = current.email.ifBlank { session.email.orEmpty() },
                    firstName = current.firstName.ifBlank { session.firstName.orEmpty() },
                    lastName = current.lastName.ifBlank { session.lastName.orEmpty() },
                    company = current.company.ifBlank { session.company.orEmpty() },
                    contactPersonFullName = current.contactPersonFullName.ifBlank { sessionFullName },
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onContactPersonFullNameChange(value: String) {
        _uiState.update { it.copy(contactPersonFullName = value) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value) }
    }

    fun onCompanyChange(value: String) {
        _uiState.update { it.copy(company = value) }
    }

    fun onVatNumberChange(value: String) {
        _uiState.update { it.copy(vatNumber = value) }
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
        _uiState.update { current ->
            val hasChanged = current.countryIso != country.isoCode
            current.copy(
                country = country.displayName,
                countryIso = country.isoCode,
                city = if (hasChanged) "" else current.city,
                street = if (hasChanged) "" else current.street,
                postalCode = if (hasChanged) "" else current.postalCode,
            )
        }
    }

    fun onCityChange(value: String) {
        _uiState.update { current ->
            val hasChanged = current.city.trim() != value.trim()
            current.copy(
                city = value,
                street = if (hasChanged) "" else current.street,
                postalCode = if (hasChanged) "" else current.postalCode,
            )
        }
    }

    fun onStreetChange(value: String) {
        _uiState.update { it.copy(street = value) }
    }

    fun onPostalCodeChange(value: String) {
        _uiState.update { it.copy(postalCode = value) }
    }

    fun onAddressCoordinatesChange(value: String) {
        _uiState.update { it.copy(addressCoordinates = value) }
    }

    fun onCompanyRegistrationChange(value: String) {
        _uiState.update { it.copy(companyRegistrationFileName = value) }
    }

    fun onInvoiceChange(value: String) {
        _uiState.update { it.copy(invoiceFileName = value) }
    }

    fun onCustomerDataPrivacyAcceptedChange(value: Boolean) {
        _uiState.update { it.copy(customerDataPrivacyAccepted = value) }
    }

    fun onTermsAndPrivacyAcceptedChange(value: Boolean) {
        _uiState.update { it.copy(termsAndPrivacyAccepted = value) }
    }

    fun onNewsletterChange(value: Boolean) {
        _uiState.update { it.copy(newsletter = value) }
    }

    fun submit() {
        val currentState = _uiState.value
        if (!currentState.isSubmitEnabled) {
            return
        }

        val countryIso = currentState.countryIso.ifBlank { defaultCountryIso() }

        _uiState.update { it.copy(status = WholesaleApplicationStatus.Loading) }
        viewModelScope.launch {
            val result = submitWholesaleApplicationUseCase(
                WholesaleApplicationParams(
                    customerId = currentState.customerId,
                    email = currentState.email.trim(),
                    firstName = currentState.firstName.trim(),
                    lastName = currentState.lastName.trim(),
                    contactPersonFullName = currentState.contactPersonFullName.trim(),
                    company = currentState.company.trim(),
                    vatNumber = currentState.vatNumber.trim(),
                    country = currentState.country.trim().ifBlank { countryIso },
                    countryIso = countryIso,
                    street = currentState.street.trim(),
                    city = currentState.city.trim(),
                    postalCode = currentState.postalCode.trim(),
                    phone = currentState.phone.trim(),
                    addressCoordinates = currentState.addressCoordinates.trim().ifBlank { null },
                    companyRegistrationFileName = currentState.companyRegistrationFileName.trim().ifBlank { null },
                    invoiceFileName = currentState.invoiceFileName.trim().ifBlank { null },
                    customerDataPrivacyAccepted = currentState.customerDataPrivacyAccepted,
                    termsAndPrivacyAccepted = currentState.termsAndPrivacyAccepted,
                    newsletter = currentState.newsletter,
                )
            )

            _uiState.update {
                when (result) {
                    is RegisterOutcome.Success -> it.copy(
                        status = WholesaleApplicationStatus.Success(result.result.message),
                    )
                    is RegisterOutcome.Error -> it.copy(
                        status = WholesaleApplicationStatus.Error(result.message),
                    )
                }
            }
        }
    }

    private fun defaultCountryIso(): String {
        val locale = Locale.getDefault()
        val region = locale.country.uppercase(Locale.ROOT)
        if (region in setOf("GR", "SE")) {
            return region
        }
        return if (locale.language.equals("sv", ignoreCase = true)) "SE" else "GR"
    }
}

data class WholesaleApplicationUiState(
    val customerId: Int? = null,
    val isEligible: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val contactPersonFullName: String = "",
    val phone: String = "",
    val company: String = "",
    val vatNumber: String = "",
    val country: String = "",
    val countryIso: String = "",
    val city: String = "",
    val street: String = "",
    val postalCode: String = "",
    val addressCoordinates: String = "",
    val companyRegistrationFileName: String = "",
    val invoiceFileName: String = "",
    val customerDataPrivacyAccepted: Boolean = false,
    val termsAndPrivacyAccepted: Boolean = false,
    val newsletter: Boolean = false,
    val status: WholesaleApplicationStatus = WholesaleApplicationStatus.Idle,
) {
    val isSubmitEnabled: Boolean
        get() = isEligible &&
            status !is WholesaleApplicationStatus.Loading &&
            firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            contactPersonFullName.isNotBlank() &&
            phone.isNotBlank() &&
            company.isNotBlank() &&
            vatNumber.isNotBlank() &&
            countryIso.isNotBlank() &&
            city.isNotBlank() &&
            street.isNotBlank() &&
            postalCode.isNotBlank() &&
            customerDataPrivacyAccepted &&
            termsAndPrivacyAccepted
}

sealed interface WholesaleApplicationStatus {
    data object Idle : WholesaleApplicationStatus
    data object Loading : WholesaleApplicationStatus
    data class Success(val message: String) : WholesaleApplicationStatus
    data class Error(val message: String) : WholesaleApplicationStatus
}

private data class SessionProfile(
    val customerId: Int?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val company: String?,
    val canViewPrices: Boolean,
)

private data class ProfileSnapshot(
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val company: String?,
)
