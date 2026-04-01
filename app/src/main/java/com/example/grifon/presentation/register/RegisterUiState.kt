package com.example.grifon.presentation.register

data class RegisterUiState(
    val socialTitle: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val contactPersonFullName: String = "",
    val phone: String = "",
    val iban: String = "",
    val email: String = "",
    val emailConfirmation: String = "",
    val companyName: String = "",
    val vatNumber: String = "",
    val addressCoordinates: String = "",
    val companyRegistrationFileName: String? = null,
    val invoiceFileName: String? = null,
    val country: String = "",
    val countryIso: String = "",
    val city: String = "",
    val street: String = "",
    val postalCode: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val customerDataPrivacyAccepted: Boolean = false,
    val newsletterOptIn: Boolean = false,
    val termsAndPrivacyAccepted: Boolean = false,
    val wholesaleRequested: Boolean = false,
    val googleDisplayName: String? = null,
    val googleAccountEmail: String? = null,
    val googleSignInError: String? = null,
    val status: RegisterStatus = RegisterStatus.Idle,
) {
    val isSubmitEnabled: Boolean
        get() = status !is RegisterStatus.Loading &&
            socialTitle.isNotBlank() &&
            firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            password.trim().length >= 8 &&
            termsAndPrivacyAccepted &&
            (!wholesaleRequested || (
                customerDataPrivacyAccepted
            )) &&
            (wholesaleRequested || (
                email == emailConfirmation &&
                password == passwordConfirmation
            ))
}

sealed interface RegisterStatus {
    data object Idle : RegisterStatus
    data object Loading : RegisterStatus
    data class Success(val message: String) : RegisterStatus
    data class Error(val message: String) : RegisterStatus
}
