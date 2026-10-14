package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseAuth(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedInFlow = MutableStateFlow(hasValidSession())
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    private val refreshMutex = Mutex()

    private val rawHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isLoggedIn(): Boolean {
        return _isLoggedInFlow.value
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    private fun hasValidSession(): Boolean {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        return !accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank()
    }

    suspend fun login(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Supabase URL aur Anon Key configure nahi hain. AI Studio Secrets panel mein add karein.")
            )
        }

        val baseUrl = SupabaseConfig.getSafeBaseUrl()
        val authUrl = "${baseUrl}auth/v1/token?grant_type=password"

        val jsonBody = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url(authUrl)
            .addHeader("apikey", SupabaseConfig.anonKey)
            .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = rawHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errorObj = JSONObject(bodyString)
                    errorObj.optString("error_description", errorObj.optString("msg", "Login fail ho gaya"))
                } catch (e: Exception) {
                    "Login fail: Code ${response.code}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(bodyString)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", "")
            val expiresIn = json.optLong("expires_in", 3600L)
            val expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L)

            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_EXPIRES_AT, expiresAtMillis)
                .putString(KEY_USER_EMAIL, email.trim())
                .apply()

            _isLoggedInFlow.value = true
            Result.success(accessToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getValidAccessToken(): String? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)

        if (accessToken.isNullOrBlank() && refreshToken.isNullOrBlank()) {
            return null
        }

        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val isExpiredOrClose = System.currentTimeMillis() + 60_000 >= expiresAt

        if (isExpiredOrClose && !refreshToken.isNullOrBlank()) {
            // Refresh synchronously
            return runBlocking {
                refreshMutex.withLock {
                    // Re-check inside lock
                    val currentExpiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
                    if (System.currentTimeMillis() + 60_000 < currentExpiresAt) {
                        return@withLock prefs.getString(KEY_ACCESS_TOKEN, null)
                    }
                    val refreshed = executeRefreshToken(refreshToken)
                    refreshed ?: accessToken
                }
            }
        }

        return accessToken
    }

    private fun executeRefreshToken(refreshToken: String): String? {
        if (!SupabaseConfig.isConfigured) return null

        val baseUrl = SupabaseConfig.getSafeBaseUrl()
        val authUrl = "${baseUrl}auth/v1/token?grant_type=refresh_token"

        val jsonBody = JSONObject().apply {
            put("refresh_token", refreshToken)
        }.toString()

        val request = Request.Builder()
            .url(authUrl)
            .addHeader("apikey", SupabaseConfig.anonKey)
            .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            val response = rawHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(bodyString)
                val newAccessToken = json.getString("access_token")
                val newRefreshToken = json.optString("refresh_token", refreshToken)
                val expiresIn = json.optLong("expires_in", 3600L)
                val expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L)

                prefs.edit()
                    .putString(KEY_ACCESS_TOKEN, newAccessToken)
                    .putString(KEY_REFRESH_TOKEN, newRefreshToken)
                    .putLong(KEY_EXPIRES_AT, expiresAtMillis)
                    .apply()

                _isLoggedInFlow.value = true
                newAccessToken
            } else {
                if (response.code == 400 || response.code == 401) {
                    // Invalid or expired refresh token: clear tokens and logout to redirect to login
                    logout()
                }
                null
            }
        } catch (e: Exception) {
            // Network error / timeout: do NOT logout, keep existing tokens
            null
        }
    }

    suspend fun forceRefreshToken(): String? = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            if (refreshToken.isNullOrBlank()) {
                logout()
                return@withLock null
            }
            executeRefreshToken(refreshToken)
        }
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_USER_EMAIL)
            .apply()
        _isLoggedInFlow.value = false
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_EMAIL = "user_email"
    }
}
