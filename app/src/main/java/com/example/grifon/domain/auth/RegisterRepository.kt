package com.example.grifon.domain.auth

interface RegisterRepository {
    suspend fun register(params: RegisterParams): RegisterOutcome
}
