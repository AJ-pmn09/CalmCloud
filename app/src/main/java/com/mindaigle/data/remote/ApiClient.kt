package com.mindaigle.data.remote

import com.mindaigle.data.remote.api.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Auth interceptor to add token to requests
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val token = AuthManager.getToken()
        val requestBuilder = original.newBuilder()
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        // Add headers for API requests
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("User-Agent", "MindAIgle-Android-App/1.0")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("bypass-tunnel-reminder", "true")
                .build()
            chain.proceed(newRequest)
        }
        // Increase timeouts for server connection (VM/local network)
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    // API service with configurable base URL
    // Uses ServerConfig to get the base URL dynamically
    val api: ApiService
        get() {
            val baseUrl = ServerConfig.getApiBaseUrl()
            // Ensure base URL ends with /
            val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            
            return Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
}

// Token storage and test profiles (for testing without server)
object AuthManager {
    private var context: android.content.Context? = null
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_TEST_ROLE = "test_role"
    private const val KEY_TEST_USER_NAME = "test_user_name"
    private const val KEY_TEST_EMAIL = "test_email"

    fun init(context: android.content.Context) {
        this.context = context.applicationContext
    }

    fun saveToken(newToken: String) {
        context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_TOKEN, newToken)
            ?.apply()
    }

    fun getToken(): String? {
        return context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            ?.getString(KEY_TOKEN, null)
    }

    fun clearToken() {
        context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(KEY_TOKEN)
            ?.apply()
    }

    // Test profiles: use app without server (3 students, 3 parents, 3 associates)
    fun saveTestProfile(role: String, userName: String, email: String) {
        clearToken()
        context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_TEST_ROLE, role)
            ?.putString(KEY_TEST_USER_NAME, userName)
            ?.putString(KEY_TEST_EMAIL, email)
            ?.apply()
    }

    fun isTestMode(): Boolean {
        return context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            ?.getString(KEY_TEST_ROLE, null) != null
    }

    fun getTestRole(): String? {
        return context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            ?.getString(KEY_TEST_ROLE, null)
    }

    fun getTestUserName(): String? {
        return context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            ?.getString(KEY_TEST_USER_NAME, null)
    }

    fun getTestUserEmail(): String? {
        return context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            ?.getString(KEY_TEST_EMAIL, null)
    }

    fun clearTestMode() {
        context?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(KEY_TEST_ROLE)
            ?.remove(KEY_TEST_USER_NAME)
            ?.remove(KEY_TEST_EMAIL)
            ?.apply()
    }
}
