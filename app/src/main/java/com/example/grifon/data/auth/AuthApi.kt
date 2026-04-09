package com.example.grifon.data.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): RegisterResponseDto

    @POST("v1/auth/wholesale-application")
    suspend fun submitWholesaleApplication(
        @Body request: WholesaleApplicationRequestDto
    ): RegisterResponseDto

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto
}
