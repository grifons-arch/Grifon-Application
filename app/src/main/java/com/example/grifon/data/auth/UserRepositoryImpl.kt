package com.example.grifon.data.auth

import android.content.Context
import com.example.grifon.core.LoginText
import com.example.grifon.core.ShopConfig
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.data.sync.LoginCustomerActivitySyncService
import com.example.grifon.data.repository.UserRepository
import com.example.grifon.core.loginText
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import java.io.IOException
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val preferences: ShopPreferences,
    private val loginCustomerActivitySyncService: LoginCustomerActivitySyncService,
    @ApplicationContext private val context: Context,
) : UserRepository {
    private val _isLoggedIn = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        preferences.currentCustomerId
            .map { it != null }
            .onEach { _isLoggedIn.value = it }
            .launchIn(scope)
    }

    override fun isLoggedIn(): Flow<Boolean> = _isLoggedIn

    override suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val activeShopId = preferences.activeShopId.first()
            val response = authApi.login(
                LoginRequestDto(
                    email = email,
                    password = pass,
                    countryIso = if (ShopConfig.normalizeShopId(activeShopId) == ShopConfig.SwedishShopId) "SE" else "GR",
                )
            )
            // ΔΙΟΡΘΩΣΗ: Έλεγχος του ok ΚΑΙ του idCustomer (που πλέον είναι idCustomer στο DTO)
            if (response.ok && response.idCustomer != null) {
                preferences.setCustomerSession(
                    customerId = response.idCustomer,
                    canViewPrices = response.canViewPrices == true,
                    email = response.email,
                    firstName = response.firstname,
                    lastName = response.lastname,
                    company = response.company,
                )
                runCatching {
                    loginCustomerActivitySyncService.syncAfterLogin(
                        customerId = response.idCustomer,
                        shopId = activeShopId,
                    )
                }
                _isLoggedIn.value = true
                Result.success(Unit)
            } else {
                val errorMsg = localizeLoginError(
                    errorCode = response.error,
                    message = response.message,
                )
                Result.failure(Exception(errorMsg))
            }
        } catch (e: HttpException) {
            val apiMessage = extractApiErrorMessage(e)
            Result.failure(Exception(localizeLoginError(message = apiMessage)))
        } catch (e: IOException) {
            Result.failure(Exception(loginText(context, LoginText.LoginNetworkError)))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: loginText(context, LoginText.LoginFailed)))
        }
    }

    override fun logout() {
        _isLoggedIn.value = false
        scope.launch {
            preferences.clearCustomerSession()
        }
    }

    private fun extractApiErrorMessage(exception: HttpException): String? {
        val errorBody = exception.response()?.errorBody()?.string() ?: return null
        return try {
            val error = JSONObject(errorBody).optJSONObject("error")
            error?.optString("message")?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun localizeLoginError(errorCode: String? = null, message: String? = null): String {
        val normalizedCode = errorCode?.trim()?.uppercase()
        val normalizedMessage = message?.trim()

        return when {
            normalizedCode in setOf("USER_NOT_FOUND", "INVALID_PASSWORD", "INVALID_CREDENTIALS") ->
                loginText(context, LoginText.LoginInvalidCredentials)
            normalizedCode == "ACCOUNT_INACTIVE" ->
                loginText(context, LoginText.LoginAccountInactive)
            normalizedCode == "MISSING_CREDENTIALS" ->
                loginText(context, LoginText.LoginValidationError)
            normalizedMessage.equals("Upstream auth failed", ignoreCase = true) ->
                loginText(context, LoginText.LoginInvalidCredentials)
            normalizedMessage.equals("Unexpected error", ignoreCase = true) ->
                loginText(context, LoginText.LoginFailed)
            normalizedMessage.isNullOrEmpty() ->
                loginText(context, LoginText.LoginFailed)
            else -> normalizedMessage
        }
    }
}
