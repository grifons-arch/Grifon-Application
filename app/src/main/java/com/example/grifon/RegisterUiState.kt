package com.example.grifon

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
    val postalCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val notes: String = "",
    val acceptTerms: Boolean = false,
    val subscribeNewsletter: Boolean = false,
) {
    val isSubmitEnabled: Boolean
        get() = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            phone.isNotBlank() &&
            companyName.isNotBlank() &&
            vatNumber.isNotBlank() &&
            taxOffice.isNotBlank() &&
            address.isNotBlank() &&
            city.isNotBlank() &&
            postalCode.isNotBlank() &&
            email.isNotBlank() &&
            confirmEmail.isNotBlank() &&
            email == confirmEmail &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            password == confirmPassword &&
            acceptTerms
}
