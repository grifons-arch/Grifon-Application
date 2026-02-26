package com.example.grifon.data.repository

import android.util.Log
import com.example.grifon.BuildConfig
import com.example.grifon.domain.model.User
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
    private val _userDetails = MutableStateFlow<User?>(null)
    
    private val gatewayBaseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")

    override fun isLoggedIn(): Flow<Boolean> = _isLoggedIn
    override fun getUserName(): Flow<String?> = _userName
    override fun getUserDetails(): Flow<User?> = _userDetails

    override suspend fun login(email: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        try {
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

            if (response.isSuccessful) {
                val data = JSONObject(body)
                if (data.optBoolean("ok", false)) {
                    val firstName = data.optString("firstname", "User")
                    val lastName = data.optString("lastname", "")
                    _userName.value = "$firstName $lastName".trim()
                    
                    _userDetails.value = User(
                        email = data.optString("email", email),
                        firstName = firstName,
                        lastName = lastName,
                        company = data.optString("company", ""),
                        vatNumber = data.optString("vat_number", ""),
                        newsletter = data.optInt("newsletter", 0) == 1
                    )
                    
                    _isLoggedIn.value = true
                    return@withContext true
                }
            }
            return@withContext false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateProfile(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", user.email)
                put("firstName", user.firstName)
                put("lastName", user.lastName)
                put("company", user.company)
                put("vatNumber", user.vatNumber)
                put("newsletter", user.newsletter)
            }
            
            val request = Request.Builder()
                .url("$gatewayBaseUrl/v1/auth/register") // Χρήση του ίδιου endpoint που κάνει upsert
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                _userDetails.value = user
                _userName.value = "${user.firstName} ${user.lastName}"
                return@withContext true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun logout() {
        _isLoggedIn.value = false
        _userName.value = null
        _userDetails.value = null
    }
}
