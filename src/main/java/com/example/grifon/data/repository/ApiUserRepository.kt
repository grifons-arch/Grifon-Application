package com.example.grifon.data.repository

import android.util.Log
import com.example.grifon.BuildConfig
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ApiUserRepository @Inject constructor(
    private val client: OkHttpClient
) : UserRepository {

    private val _isLoggedIn = MutableStateFlow(false)
    private val _userName = MutableStateFlow<String?>(null)
    
    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    override fun isLoggedIn(): Flow<Boolean> = _isLoggedIn
    override fun getUserName(): Flow<String?> = _userName

    override suspend fun login(email: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("LoginDebug", "Attempting real login for: $email")
            
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("action", "login")
            }
            
            val request = Request.Builder()
                .url("$gatewayBaseUrl/v1/auth/login")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            Log.d("LoginDebug", "PrestaShop Response: $body")

            if (response.isSuccessful) {
                val data = JSONObject(body)
                if (data.optBoolean("ok", false)) {
                    val firstName = data.optString("firstname", "User")
                    val lastName = data.optString("lastname", "")
                    _userName.value = "$firstName $lastName".trim()
                    _isLoggedIn.value = true
                    return@withContext true
                }
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e("LoginDebug", "Login failed with error", e)
            false
        }
    }

    override suspend fun logout() {
        _isLoggedIn.value = false
        _userName.value = null
    }
}
