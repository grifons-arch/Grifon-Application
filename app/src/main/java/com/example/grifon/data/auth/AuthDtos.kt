package com.example.grifon.data.auth

import com.squareup.moshi.Json

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class LoginResponseDto(
    @field:Json(name = "ok")
    val ok: Boolean = false,
    @field:Json(name = "id_customer")
    val idCustomer: Any? = null, // Χρήση Any για να αποφύγουμε σφάλματα τύπου (String vs Int)
    val firstname: String? = null,
    val lastname: String? = null,
    val email: String? = null,
    val company: String? = null,
    val error: String? = null,
    val message: String? = null
)
