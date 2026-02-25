package com.example.grifon.domain.usecase

import com.example.grifon.data.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String, pass: String): Boolean {
        // Εδώ καλούμε το repository το οποίο με τη σειρά του καλεί το PrestaShop
        return true // Προσωρινά επιστρέφουμε true για να προχωρήσει το UI
    }
}
