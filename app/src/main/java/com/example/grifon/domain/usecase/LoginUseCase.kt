package com.example.grifon.domain.usecase

import android.util.Log
import com.example.grifon.data.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String, pass: String): Boolean {
        Log.d("LoginDebug", "LoginUseCase: Invoking login for $email")
        val result = userRepository.login(email, pass)
        Log.d("LoginDebug", "LoginUseCase: Result is $result")
        return result.isSuccess
    }
}
