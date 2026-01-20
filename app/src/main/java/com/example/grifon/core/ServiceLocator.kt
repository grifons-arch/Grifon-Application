package com.example.grifon.core

import com.example.grifon.BuildConfig
import com.example.grifon.data.auth.AuthApi
import com.example.grifon.data.auth.RegisterRepositoryImpl
import com.example.grifon.domain.auth.RegisterUseCase
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ServiceLocator {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    private val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    private val registerRepository by lazy {
        RegisterRepositoryImpl(authApi)
    }

    fun provideRegisterUseCase(): RegisterUseCase = RegisterUseCase(registerRepository)
}
