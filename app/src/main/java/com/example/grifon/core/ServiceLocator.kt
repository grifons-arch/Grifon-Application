package com.example.grifon.core

import com.example.grifon.BuildConfig
import com.example.grifon.data.auth.AuthApi
import com.example.grifon.data.auth.RegisterRepositoryImpl
import com.example.grifon.domain.auth.RegisterUseCase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ServiceLocator {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private val retrofit: Retrofit by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
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
