package com.example.grifon.data.auth

import android.util.Log
import com.example.grifon.domain.auth.RegisterOutcome
import com.example.grifon.domain.auth.RegisterParams
import com.example.grifon.domain.auth.RegisterRepository
import com.example.grifon.domain.auth.RegisterResult
import retrofit2.HttpException
import java.io.IOException
import org.json.JSONObject

class RegisterRepositoryImpl(
    private val api: AuthApi,
) : RegisterRepository {
    override suspend fun register(params: RegisterParams): RegisterOutcome {
        return try {
            Log.d(
                TAG,
                    "Register request: email=${params.email}, socialTitle=${params.socialTitle}, " +
                    "firstName=${params.firstName}, lastName=${params.lastName}, countryIso=${params.countryIso}, " +
                    "street=${params.street}, city=${params.city}, postalCode=${params.postalCode}, " +
                    "phone=${params.phone}, company=${params.company}, vatNumber=${params.vatNumber}, " +
                    "iban=${params.iban}, " +
                    "customerDataPrivacyAccepted=${params.customerDataPrivacyAccepted}, " +
                    "newsletter=${params.newsletter}, termsAndPrivacyAccepted=${params.termsAndPrivacyAccepted}, " +
                    "partnerOffers=${params.partnerOffers}",
            )
            val response = api.register(
                RegisterRequestDto(
                    email = params.email,
                    password = params.password,
                    legacyPassword = params.password,
                    socialTitle = params.socialTitle,
                    firstName = params.firstName,
                    lastName = params.lastName,
                    countryIso = params.countryIso,
                    street = params.street,
                    city = params.city,
                    postalCode = params.postalCode,
                    phone = params.phone,
                    company = params.company,
                    vatNumber = params.vatNumber,
                    iban = params.iban,
                    customerDataPrivacyAccepted = params.customerDataPrivacyAccepted,
                    newsletter = params.newsletter,
                    termsAndPrivacyAccepted = params.termsAndPrivacyAccepted,
                    partnerOffers = params.partnerOffers,
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
            val apiMessage = extractApiErrorMessage(exception)
            RegisterOutcome.Error(apiMessage ?: mapHttpError(exception.code()))
        } catch (exception: IOException) {
            Log.w(TAG, "Register request failed due to network error.", exception)
            RegisterOutcome.Error("Δεν ήταν δυνατή η σύνδεση με τον server. Δοκιμάστε ξανά.")
        } catch (exception: Exception) {
            Log.e(TAG, "Register request failed with unexpected error.", exception)
            RegisterOutcome.Error("Η εγγραφή απέτυχε. Δοκιμάστε ξανά.")
        }
    }

    private fun extractApiErrorMessage(exception: HttpException): String? {
        val errorBody = exception.response()?.errorBody()?.string() ?: return null
        Log.w(TAG, "Register request error body: $errorBody")
        return try {
            val message = JSONObject(errorBody)
                .optJSONObject("error")
                ?.optString("message")
                ?.trim()
            message?.takeIf { it.isNotEmpty() }
        } catch (exception: Exception) {
            Log.w(TAG, "Register request error body was not valid JSON.", exception)
            null
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
