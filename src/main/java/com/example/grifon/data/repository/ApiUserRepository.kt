package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.LoginRequestDto
import com.example.grifon.data.local.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ApiUserRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    private val userPreferences: UserPreferences
) : UserRepository {
    override fun isLoggedIn(): Flow<Boolean> = userPreferences.isLoggedIn

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = catalogApi.login(LoginRequestDto(email, password))
            if (response.token != null && response.customerId != null) {
                userPreferences.saveAuthData(response.token, response.customerId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid response from server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        userPreferences.clearAuthData()
    }
}
