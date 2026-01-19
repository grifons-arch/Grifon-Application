package com.example.grifon

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val companyName: String = "",
    val vatNumber: String = "",
    val taxOffice: String = "",
    val address: String = "",
    val city: String = "",
    val postalCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val notes: String = "",
    val acceptTerms: Boolean = false,
    val subscribeNewsletter: Boolean = false,
    val googleDisplayName: String? = null,
    val googleAccountEmail: String? = null,
    val googleSignInError: String? = null,
) {
    val isSubmitEnabled: Boolean
        get() = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            phone.isNotBlank() &&
            companyName.isNotBlank() &&
            vatNumber.isNotBlank() &&
            taxOffice.isNotBlank() &&
            address.isNotBlank() &&
            city.isNotBlank() &&
            postalCode.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            password == confirmPassword &&
            acceptTerms
}
