package com.example.grifon.domain.auth

data class RegisterResult(
    val customerId: String,
    val status: String,
    val message: String,
)

sealed interface RegisterOutcome {
    data class Success(val result: RegisterResult) : RegisterOutcome
    data class Error(val message: String) : RegisterOutcome
}
