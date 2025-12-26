package com.example.calmcloud.data.remote.dto

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: Int,
    @SerializedName("email")
    val email: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("role")
    val role: String,
    @SerializedName("created_at")
    val createdAt: String?
)

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class SignupRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("studentEmail")
    val studentEmail: String? = null
)

data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("token")
    val token: String,
    @SerializedName("user")
    val user: User,
    @SerializedName("userId")
    val userId: Int? = null
)

