package com.example.grifon.data.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    // Προσθήκη v1/ για να συμβαδίζει με τον Gateway
    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): RegisterResponseDto
}
