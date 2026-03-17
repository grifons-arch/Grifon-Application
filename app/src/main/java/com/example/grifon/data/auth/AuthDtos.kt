package com.example.grifon.data.auth

import com.squareup.moshi.Json

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class LoginResponseDto(
    @field:Json(name = "ok")
    val ok: Boolean,
    @field:Json(name = "id_customer")
    val customerId: Int?,
    val firstname: String?,
    val lastname: String?,
    val email: String?,
    val company: String?,
    val error: String?,
    val message: String?
)
