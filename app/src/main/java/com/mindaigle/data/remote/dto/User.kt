package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: Int,
    @SerializedName("email")
    val email: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("first_name")
    val firstName: String? = null,
    @SerializedName("last_name")
    val lastName: String? = null,
    @SerializedName("role")
    val role: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("profilePictureUrl")
    val profilePictureUrl: String? = null
) {
    /** Display name: name, or "first last", or email prefix. */
    fun displayName(): String = name?.takeIf { it.isNotBlank() }
        ?: listOfNotNull(firstName, lastName).joinToString(" ").trim().takeIf { it.isNotBlank() }
        ?: email.substringBefore("@")
        ?: "User"
    }

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

/** Some backends return user inside a "data" object. */
data class AuthResponseData(
    @SerializedName("user")
    val user: User? = null
)

data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("accessToken")
    val accessTokenCamel: String? = null,
    @SerializedName("user")
    val user: User? = null,
    @SerializedName("data")
    val data: AuthResponseData? = null,
    @SerializedName("userId")
    val userId: Int? = null
) {
    /** Token to use for auth: supports token, access_token, accessToken. */
    fun getAuthToken(): String? = token?.takeIf { it.isNotBlank() }
        ?: accessToken?.takeIf { it.isNotBlank() }
        ?: accessTokenCamel?.takeIf { it.isNotBlank() }

    /** User from top-level "user" or from "data.user" (VM shape). Use this for login/signup UI. */
    fun resolvedUser(): User? = user ?: data?.user
}

data class ProfileUpdateResponse(
    @SerializedName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerializedName("name") val name: String? = null
)

data class ProfilePhotoResponse(
    @SerializedName("profilePictureUrl") val profilePictureUrl: String
)

