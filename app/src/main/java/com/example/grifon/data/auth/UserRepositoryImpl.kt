package com.example.grifon.data.auth

import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val preferences: ShopPreferences
) : UserRepository {
    private val _isLoggedIn = MutableStateFlow(false)

    override fun isLoggedIn(): Flow<Boolean> = _isLoggedIn

    override suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val response = authApi.login(LoginRequestDto(email, pass))
            // ΔΙΟΡΘΩΣΗ: Έλεγχος του ok ΚΑΙ του idCustomer (που πλέον είναι idCustomer στο DTO)
            if (response.ok && response.idCustomer != null) {
                _isLoggedIn.value = true
                Result.success(Unit)
            } else {
                val errorMsg = response.error ?: response.message ?: "Λάθος στοιχεία σύνδεσης"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        _isLoggedIn.value = false
    }
}
