package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.*

class CommunicationRepository {
    private val api = ApiClient.api

    suspend fun sendMessage(
        recipientType: String,
        recipientId: Int? = null,
        recipientCohort: String? = null,
        subject: String? = null,
        message: String,
        priority: String = "normal",
        parentVisible: Boolean = true,
        emergencyOverride: Boolean = false
    ): Result<SendMessageResponse> {
        return try {
            val response = api.sendMessage(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                SendMessageRequest(recipientType, recipientId, recipientCohort, subject, message, priority, parentVisible, emergencyOverride)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun errorMessage(response: retrofit2.Response<*>): String {
        val body = response.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            return try {
                org.json.JSONObject(body).optString("error").takeIf { it.isNotEmpty() }
                    ?: (response.message() ?: "Failed to get messages")
            } catch (_: Exception) {
                response.message() ?: "Failed to get messages"
            }
        }
        return response.message() ?: "Failed to get messages"
    }

    suspend fun getMyMessages(): Result<List<Communication>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockCommunicationsResponse().communications)
        val token = AuthManager.getToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not logged in"))
        return try {
            val response = api.getMyMessages("Bearer $token")
            when {
                response.code() == 404 || response.code() == 501 -> Result.success(emptyList())
                response.isSuccessful && response.body() != null -> Result.success(response.body()!!.communications ?: emptyList())
                else -> Result.failure(Exception(errorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessageAsRead(messageId: Int): Result<MessageActionResponse> {
        return try {
            val response = api.markMessageAsRead(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                messageId
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to mark message as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSentMessages(limit: Int? = null, offset: Int? = null): Result<Pair<List<SentMessage>, Int>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockSentMessagesResponse())
        return try {
            val response = api.getSentMessages(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                limit,
                offset
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(Pair(body.communications, body.total))
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get sent messages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getParentVisibleMessages(studentId: Int): Result<List<Communication>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockCommunicationsResponse().communications)
        return try {
            val response = api.getParentVisibleMessages(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                studentId
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.communications)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get parent visible messages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

