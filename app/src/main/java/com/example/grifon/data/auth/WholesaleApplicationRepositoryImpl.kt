package com.example.grifon.data.auth

import android.util.Log
import com.example.grifon.domain.auth.RegisterOutcome
import com.example.grifon.domain.auth.RegisterResult
import com.example.grifon.domain.auth.WholesaleApplicationParams
import com.example.grifon.domain.auth.WholesaleApplicationRepository
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class WholesaleApplicationRepositoryImpl(
    private val api: AuthApi,
) : WholesaleApplicationRepository {
    override suspend fun submit(params: WholesaleApplicationParams): RegisterOutcome {
        return try {
            val response = api.submitWholesaleApplication(
                WholesaleApplicationRequestDto(
                    customerId = params.customerId,
                    email = params.email,
                    firstName = params.firstName,
                    lastName = params.lastName,
                    contactPersonFullName = params.contactPersonFullName,
                    company = params.company,
                    vatNumber = params.vatNumber,
                    country = params.country,
                    countryIso = params.countryIso,
                    street = params.street,
                    city = params.city,
                    postalCode = params.postalCode,
                    phone = params.phone,
                    addressCoordinates = params.addressCoordinates,
                    companyRegistrationFileName = params.companyRegistrationFileName,
                    invoiceFileName = params.invoiceFileName,
                    customerDataPrivacyAccepted = params.customerDataPrivacyAccepted,
                    termsAndPrivacyAccepted = params.termsAndPrivacyAccepted,
                    newsletter = params.newsletter,
                )
            )
            RegisterOutcome.Success(
                RegisterResult(
                    customerId = response.customerId,
                    status = response.status,
                    message = response.message,
                )
            )
        } catch (exception: HttpException) {
            Log.w(TAG, "Wholesale application failed with HTTP ${exception.code()}.", exception)
            val apiMessage = extractApiErrorMessage(exception)
            RegisterOutcome.Error(apiMessage ?: mapHttpError(exception.code()))
        } catch (exception: IOException) {
            Log.w(TAG, "Wholesale application failed due to network error.", exception)
            RegisterOutcome.Error("Δεν ήταν δυνατή η σύνδεση με τον server. Δοκιμάστε ξανά.")
        } catch (exception: Exception) {
            Log.e(TAG, "Wholesale application failed with unexpected error.", exception)
            RegisterOutcome.Error("Η αίτηση χονδρικής απέτυχε. Δοκιμάστε ξανά.")
        }
    }

    private fun extractApiErrorMessage(exception: HttpException): String? {
        val errorBody = exception.response()?.errorBody()?.string() ?: return null
        return try {
            JSONObject(errorBody)
                .optJSONObject("error")
                ?.optString("message")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun mapHttpError(code: Int): String {
        return when (code) {
            400 -> "Ελέγξτε τα στοιχεία της αίτησης."
            401 -> "Δεν επιτρέπεται η υποβολή της αίτησης."
            409 -> "Υπάρχει ήδη καταχωρημένη αίτηση για αυτό το email."
            else -> "Η αίτηση χονδρικής απέτυχε. Δοκιμάστε ξανά."
        }
    }

    private companion object {
        private const val TAG = "WholesaleApplication"
    }
}
