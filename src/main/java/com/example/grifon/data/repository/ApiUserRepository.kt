package com.example.grifon.data.repository

import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.ErrorResponseDto
import com.example.grifon.data.catalog.LoginRequestDto
import com.example.grifon.data.local.UserPreferences
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import javax.inject.Inject

class ApiUserRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    private val userPreferences: UserPreferences,
    private val moshi: Moshi
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
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = try {
                if (errorBody != null) {
                    val adapter = moshi.adapter(ErrorResponseDto::class.java)
                    adapter.fromJson(errorBody)?.error?.message ?: "Σφάλμα σύνδεσης"
                } else {
                    "Σφάλμα σύνδεσης"
                }
            } catch (jsonException: Exception) {
                "Σφάλμα σύνδεσης"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception("Πρόβλημα δικτύου. Δοκιμάστε ξανά."))
        }
    }

    override suspend fun logout() {
        userPreferences.clearAuthData()
    }
}
