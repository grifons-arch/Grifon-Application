package com.example.grifon.data.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): RegisterResponseDto
}
