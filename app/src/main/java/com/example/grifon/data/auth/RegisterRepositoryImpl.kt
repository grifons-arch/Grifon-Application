package com.example.grifon.data.auth

import com.example.grifon.domain.auth.RegisterOutcome
import com.example.grifon.domain.auth.RegisterParams
import com.example.grifon.domain.auth.RegisterRepository
import com.example.grifon.domain.auth.RegisterResult
import retrofit2.HttpException
import java.io.IOException

class RegisterRepositoryImpl(
    private val api: AuthApi,
) : RegisterRepository {
    override suspend fun register(params: RegisterParams): RegisterOutcome {
        return try {
            val response = api.register(
                RegisterRequestDto(
                    email = params.email,
                    password = params.password,
                    firstName = params.firstName,
                    lastName = params.lastName,
                    countryIso = params.countryIso,
                    phone = params.phone,
                    company = params.company,
                ),
            )
            RegisterOutcome.Success(
                RegisterResult(
                    customerId = response.customerId,
                    status = response.status,
                    message = response.message,
                ),
            )
        } catch (exception: HttpException) {
            RegisterOutcome.Error(mapHttpError(exception.code()))
        } catch (exception: IOException) {
            RegisterOutcome.Error("Δεν ήταν δυνατή η σύνδεση με τον server. Δοκιμάστε ξανά.")
        } catch (exception: Exception) {
            RegisterOutcome.Error("Η εγγραφή απέτυχε. Δοκιμάστε ξανά.")
        }
    }

    private fun mapHttpError(code: Int): String {
        return when (code) {
            400 -> "Ελέγξτε τα στοιχεία που συμπληρώσατε."
            409 -> "Το email χρησιμοποιείται ήδη."
            else -> "Η εγγραφή απέτυχε. Δοκιμάστε ξανά."
        }
    }
}
