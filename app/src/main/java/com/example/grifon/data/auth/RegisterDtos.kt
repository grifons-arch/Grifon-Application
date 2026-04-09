package com.example.grifon.data.auth

import com.squareup.moshi.Json

data class RegisterRequestDto(
    val email: String,
    val password: String,
    @field:Json(name = "passwd")
    val passwd: String = password,
    val socialTitle: String? = null,
    val firstName: String,
    val lastName: String,
    val contactPersonFullName: String? = null,
    val countryIso: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val phone: String? = null,
    val company: String? = null,
    val vatNumber: String? = null,
    val addressCoordinates: String? = null,
    val iban: String? = null,
    val companyRegistrationFileName: String? = null,
    val invoiceFileName: String? = null,
    val customerDataPrivacyAccepted: Boolean = false,
    val newsletter: Boolean = false,
    val termsAndPrivacyAccepted: Boolean = false,
    val wholesaleRequested: Boolean = false,
    val partnerOffers: Boolean? = null,
)

data class RegisterResponseDto(
    val customerId: String,
    val status: String,
    val message: String,
)
