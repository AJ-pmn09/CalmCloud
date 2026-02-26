package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.*

class ReminderRepository {
    private val api = ApiClient.api

    suspend fun getReminderConfig(): Result<ReminderConfigResponse> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockReminderConfigResponse())
        return try {
            val response = api.getReminderConfig(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}"
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get reminder config"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReminderConfig(
        reminderEnabled: Boolean? = null,
        reminderIntervalHours: Int? = null,
        smartScheduling: Boolean? = null,
        quietHoursStart: String? = null,
        quietHoursEnd: String? = null,
        maxPerDay: Int? = null
    ): Result<UpdateReminderConfigResponse> {
        return try {
            val response = api.updateReminderConfig(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                UpdateReminderConfigRequest(reminderEnabled, reminderIntervalHours, smartScheduling, quietHoursStart, quietHoursEnd, maxPerDay)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update reminder config"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReminderLogs(
        studentId: Int? = null,
        escalationLevel: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<List<ReminderLog>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockReminderLogsResponse().logs)
        return try {
            val response = api.getReminderLogs(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                studentId,
                escalationLevel,
                limit,
                offset
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.logs)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get reminder logs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

