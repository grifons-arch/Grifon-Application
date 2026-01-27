package com.example.grifon.data.auth

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val socialTitle: String? = null,
    val firstName: String,
    val lastName: String,
    val countryIso: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val phone: String? = null,
    val company: String? = null,
    val customerDataPrivacyAccepted: Boolean = false,
    val newsletter: Boolean = false,
    val termsAndPrivacyAccepted: Boolean = false,
    val partnerOffers: Boolean? = null,
)

data class RegisterResponseDto(
    val customerId: String,
    val status: String,
    val message: String,
)
