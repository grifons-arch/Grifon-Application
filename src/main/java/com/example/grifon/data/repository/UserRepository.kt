package com.example.grifon.data.repository

import com.example.grifon.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun isLoggedIn(): Flow<Boolean>
    fun getUserName(): Flow<String?>
    fun getUserDetails(): Flow<User?>
    suspend fun login(email: String, pass: String): Boolean
    suspend fun logout()
    suspend fun updateProfile(user: User): Boolean // Προσθήκη για ενημέρωση
}
