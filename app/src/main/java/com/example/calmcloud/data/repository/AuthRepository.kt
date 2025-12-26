package com.example.calmcloud.data.repository

import com.example.calmcloud.data.remote.ApiClient
import com.example.calmcloud.data.remote.AuthManager
import com.example.calmcloud.data.remote.dto.*
import retrofit2.Response

class AuthRepository {
    private val api = ApiClient.api

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                AuthManager.saveToken(authResponse.token)
                Result.success(authResponse)
            } else {
                // Parse error from response
                val errorMsg = when {
                    response.code() == 401 -> "Invalid email or password"
                    response.code() == 500 -> "Server error. Please try again."
                    else -> {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.contains("error")) {
                            try {
                                errorBody.substringAfter("\"error\":\"").substringBefore("\"")
                            } catch (e: Exception) {
                                "Login failed: ${response.message()}"
                            }
                        } else {
                            "Login failed: ${response.message()}"
                        }
                    }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("Unable to resolve host") == true -> "Cannot connect to server. Is backend running?"
                e.message?.contains("Failed to connect") == true -> "Cannot connect to server. Check your connection."
                else -> "Error: ${e.message ?: e.localizedMessage ?: "Unknown error"}"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun signup(
        email: String,
        password: String,
        name: String,
        role: String,
        studentEmail: String? = null
    ): Result<AuthResponse> {
        return try {
            val response = api.signup(
                SignupRequest(email, password, name, role, studentEmail)
            )
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                AuthManager.saveToken(authResponse.token)
                Result.success(authResponse)
            } else {
                Result.failure(Exception(response.message() ?: "Signup failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<User> {
        return try {
            val token = AuthManager.getToken()
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }
            val response = api.getProfile("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        AuthManager.clearToken()
    }

    fun isAuthenticated(): Boolean {
        return AuthManager.getToken() != null
    }
}

