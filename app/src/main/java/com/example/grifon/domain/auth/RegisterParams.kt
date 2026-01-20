package com.example.grifon.domain.auth

data class RegisterParams(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val countryIso: String,
    val phone: String? = null,
    val company: String? = null,
)
