package com.example.grifon.presentation.register

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val confirmEmail: String = "",
    val companyName: String = "",
    val vatNumber: String = "",
    val taxOffice: String = "",
    val address: String = "",
    val city: String = "",
    val countryName: String = "",
    val countryIso: String = "",
    val postalCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val notes: String = "",
    val acceptTerms: Boolean = false,
    val subscribeNewsletter: Boolean = false,
    val googleDisplayName: String? = null,
    val googleAccountEmail: String? = null,
    val googleSignInError: String? = null,
    val status: RegisterStatus = RegisterStatus.Idle,
) {
    val isSubmitEnabled: Boolean
        get() = status !is RegisterStatus.Loading &&
            firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            confirmEmail.isNotBlank() &&
            email == confirmEmail &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            password == confirmPassword &&
            countryIso.isNotBlank() &&
            acceptTerms
}

sealed interface RegisterStatus {
    data object Idle : RegisterStatus
    data object Loading : RegisterStatus
    data class Success(val message: String) : RegisterStatus
    data class Error(val message: String) : RegisterStatus
}
