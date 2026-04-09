package com.example.grifon.domain.auth

interface WholesaleApplicationRepository {
    suspend fun submit(params: WholesaleApplicationParams): RegisterOutcome
}
