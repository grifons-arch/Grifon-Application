package com.example.grifon.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.grifon.domain.auth.RegisterUseCase

class RegisterViewModelFactory(
    private val registerUseCase: RegisterUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(registerUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
