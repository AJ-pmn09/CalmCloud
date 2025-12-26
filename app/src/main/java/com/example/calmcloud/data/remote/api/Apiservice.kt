package com.example.calmcloud.data.remote.api

import com.example.calmcloud.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Health check
    @GET("hello")
    suspend fun hello(): Response<Map<String, String>>

    // Authentication
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @GET("auth/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>

    // Student Data
    @POST("student-data")
    suspend fun saveStudentData(
        @Header("Authorization") token: String,
        @Body request: SaveStudentDataRequest
    ): Response<SaveStudentDataResponse>

    @GET("student-data/{studentId}")
    suspend fun getStudentData(
        @Header("Authorization") token: String,
        @Path("studentId") studentId: Int
    ): Response<StudentDataResponse>

    @GET("students")
    suspend fun getStudents(
        @Header("Authorization") token: String
    ): Response<StudentsResponse>

    // Parent endpoints
    @GET("parent/children")
    suspend fun getChildren(
        @Header("Authorization") token: String
    ): Response<ChildrenResponse>

    // Assistance Requests
    @POST("assistance-request")
    suspend fun createAssistanceRequest(
        @Header("Authorization") token: String,
        @Body request: CreateAssistanceRequest
    ): Response<CreateAssistanceResponse>

    @GET("assistance-requests")
    suspend fun getAssistanceRequests(
        @Header("Authorization") token: String
    ): Response<AssistanceRequestsResponse>

    @PUT("assistance-request/{requestId}")
    suspend fun updateAssistanceRequest(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: Int,
        @Body request: UpdateAssistanceRequest
    ): Response<UpdateAssistanceResponse>
}

