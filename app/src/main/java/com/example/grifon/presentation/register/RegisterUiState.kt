package com.example.grifon.presentation.register

data class RegisterUiState(
    val socialTitle: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val companyName: String = "",
    val vatNumber: String = "",
    val countryIso: String = "GR",
    val password: String = "",
    val acceptPrivacy: Boolean = false,
    val acceptTerms: Boolean = false,
    val subscribeNewsletter: Boolean = true,
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
            password.isNotBlank() &&
            acceptPrivacy &&
            acceptTerms
}

sealed interface RegisterStatus {
    data object Idle : RegisterStatus
    data object Loading : RegisterStatus
    data class Success(val message: String) : RegisterStatus
    data class Error(val message: String) : RegisterStatus
}
