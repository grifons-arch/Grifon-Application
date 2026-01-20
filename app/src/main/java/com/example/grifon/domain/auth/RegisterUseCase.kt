package com.example.grifon.domain.auth

class RegisterUseCase(
    private val repository: RegisterRepository,
) {
    suspend operator fun invoke(params: RegisterParams): RegisterOutcome {
        return repository.register(params)
    }
}
