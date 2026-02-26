package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.dto.*
import retrofit2.Response

class AuthRepository {
    private val api = ApiClient.api

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val token = authResponse.getAuthToken()
                if (token.isNullOrBlank()) {
                    return Result.failure(Exception("Login failed: Server did not return a token. The backend must be updated to include a JWT in the login response—contact your administrator."))
                }
                AuthManager.clearTestMode()
                AuthManager.saveToken(token)
                Result.success(authResponse)
            } else {
                // Parse error from response
                val errorMsg = when {
                    response.code() == 401 -> "Invalid email or password"
                    response.code() == 404 -> "Server returned Not Found (404). Check that the backend is running and the app uses the same URL and port as START_ALL output (e.g. 192.168.100.6:3006)."
                    response.code() == 500 -> "Server error. Please try again."
                    response.code() == 503 -> "Service Unavailable: Tunnel disconnected. Please restart tunnel on server."
                    else -> {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.contains("Tunnel Unavailable") || errorBody.contains("503")) {
                            "Service Unavailable: Tunnel disconnected. Please restart tunnel on server."
                        } else if (errorBody.contains("error")) {
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
            val isConnectionError = e is java.net.ConnectException ||
                e is java.net.UnknownHostException ||
                e is java.net.SocketTimeoutException ||
                e.message?.contains("Unable to resolve host") == true ||
                e.message?.contains("Failed to connect") == true ||
                e.message?.contains("Connection refused") == true ||
                e.message?.contains("unexpected end of stream") == true ||
                e.message?.contains("SocketTimeoutException") == true ||
                e.message?.contains("timeout") == true
            val serverUrl = com.mindaigle.data.remote.ServerConfig.getBaseUrl()
            val errorMsg = when {
                isConnectionError -> "Cannot connect to server ($serverUrl). Ensure your device is on the same Wi‑Fi as the server and the backend is running."
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
                // Check if token exists before saving
                val token = authResponse.getAuthToken()
                if (token.isNullOrBlank()) {
                    return Result.failure(Exception("Signup failed: No token received from server."))
                }
                AuthManager.clearTestMode()
                AuthManager.saveToken(token)
                Result.success(authResponse)
            } else {
                Result.failure(Exception(response.message() ?: "Signup failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<User> {
        if (AuthManager.isTestMode()) {
            val role = AuthManager.getTestRole() ?: "student"
            val name = AuthManager.getTestUserName() ?: "Test User"
            val email = AuthManager.getTestUserEmail() ?: "test@test.mindaigle"
            return Result.success(User(id = 0, email = email, name = name, role = role, createdAt = null))
        }
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

    suspend fun uploadProfilePhoto(base64Image: String): Result<String> {
        if (AuthManager.isTestMode()) return Result.failure(Exception("Not available in test mode"))
        return try {
            val token = AuthManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = api.uploadProfilePhoto("Bearer $token", mapOf("image" to base64Image))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.profilePictureUrl)
            } else {
                Result.failure(Exception(response.message() ?: "Upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        AuthManager.clearToken()
        AuthManager.clearTestMode()
    }

    fun isAuthenticated(): Boolean {
        return AuthManager.getToken() != null || AuthManager.isTestMode()
    }
}

