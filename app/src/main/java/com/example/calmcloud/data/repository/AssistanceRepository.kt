package com.example.calmcloud.data.repository

import com.example.calmcloud.data.remote.ApiClient
import com.example.calmcloud.data.remote.dto.*

class AssistanceRepository {
    private val api = ApiClient.api

    suspend fun createRequest(
        studentId: Int,
        message: String,
        urgency: String
    ): Result<CreateAssistanceResponse> {
        return try {
            val response = api.createAssistanceRequest(
                "Bearer ${com.example.calmcloud.data.remote.AuthManager.getToken()}",
                CreateAssistanceRequest(studentId, message, urgency)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to create request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRequests(): Result<List<AssistanceRequest>> {
        return try {
            val response = api.getAssistanceRequests(
                "Bearer ${com.example.calmcloud.data.remote.AuthManager.getToken()}"
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.requests)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get requests"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRequest(
        requestId: Int,
        status: String?,
        notes: String?
    ): Result<UpdateAssistanceResponse> {
        return try {
            val response = api.updateAssistanceRequest(
                "Bearer ${com.example.calmcloud.data.remote.AuthManager.getToken()}",
                requestId,
                UpdateAssistanceRequest(status, notes)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

