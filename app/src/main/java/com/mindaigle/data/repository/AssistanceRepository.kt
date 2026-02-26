package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.*

class AssistanceRepository {
    private val api = ApiClient.api

    suspend fun createRequest(
        studentIds: Any, // Can be Int, List<Int>, or "all"
        message: String,
        urgency: String
    ): Result<CreateAssistanceResponse> {
        return try {
            val response = api.createAssistanceRequest(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                CreateAssistanceRequest(studentIds, message, urgency)
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
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockAssistanceRequestsResponse().requests)
        return try {
            val response = api.getAssistanceRequests(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}"
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
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
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

