package com.example.grifon.data.auth

import android.util.Log
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
            Log.w(TAG, "Register request failed with HTTP ${exception.code()}.", exception)
            RegisterOutcome.Error(mapHttpError(exception.code()))
        } catch (exception: IOException) {
            Log.w(TAG, "Register request failed due to network error.", exception)
            RegisterOutcome.Error("Δεν ήταν δυνατή η σύνδεση με τον server. Δοκιμάστε ξανά.")
        } catch (exception: Exception) {
            Log.e(TAG, "Register request failed with unexpected error.", exception)
            RegisterOutcome.Error("Η εγγραφή απέτυχε. Δοκιμάστε ξανά.")
        }
    }

    private fun mapHttpError(code: Int): String {
        return when (code) {
            400 -> "Ελέγξτε τα στοιχεία που συμπληρώσατε."
            401 -> "Δεν επιτρέπεται η εγγραφή με αυτά τα στοιχεία."
            409 -> "Το email χρησιμοποιείται ήδη."
            else -> "Η εγγραφή απέτυχε. Δοκιμάστε ξανά."
        }
    }

    private companion object {
        private const val TAG = "RegisterRepository"
    }
}
