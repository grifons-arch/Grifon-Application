package com.example.grifon.domain.auth

data class WholesaleApplicationParams(
    val customerId: Int? = null,
    val email: String,
    val firstName: String,
    val lastName: String,
    val contactPersonFullName: String,
    val company: String,
    val vatNumber: String,
    val country: String,
    val countryIso: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val phone: String,
    val addressCoordinates: String? = null,
    val companyRegistrationFileName: String? = null,
    val invoiceFileName: String? = null,
    val customerDataPrivacyAccepted: Boolean,
    val termsAndPrivacyAccepted: Boolean,
    val newsletter: Boolean = false,
)
