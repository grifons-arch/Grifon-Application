package com.example.grifon.domain.auth

data class RegisterParams(
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
    val newsletter: Boolean? = null,
    val partnerOffers: Boolean? = null,
)
