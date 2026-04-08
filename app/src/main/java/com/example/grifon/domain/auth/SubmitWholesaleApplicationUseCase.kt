package com.example.grifon.domain.auth

class SubmitWholesaleApplicationUseCase(
    private val repository: WholesaleApplicationRepository,
) {
    suspend operator fun invoke(params: WholesaleApplicationParams): RegisterOutcome {
        return repository.submit(params)
    }
}
