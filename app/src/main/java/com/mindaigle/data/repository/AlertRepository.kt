package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.*

class AlertRepository {
    private val api = ApiClient.api

    suspend fun createEmergencyAlert(
        alertType: String = "emergency",
        message: String? = null,
        suicideRiskScreening: SuicideRiskScreening? = null
    ): Result<CreateEmergencyAlertResponse> {
        return try {
            val response = api.createEmergencyAlert(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                CreateEmergencyAlertRequest(alertType, message, suicideRiskScreening)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to create alert"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSuicideRiskScreening(alertId: Int): Result<SuicideRiskScreeningResponse> {
        return try {
            val response = api.getSuicideRiskScreening(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                alertId
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get screening"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEmergencyAlerts(status: String? = null): Result<List<EmergencyAlert>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockEmergencyAlertsResponse().alerts)
        return try {
            val response = api.getEmergencyAlerts(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                status
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.alerts)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get alerts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acknowledgeAlert(alertId: Int): Result<AlertActionResponse> {
        return try {
            val response = api.acknowledgeAlert(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                alertId
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to acknowledge alert"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveAlert(alertId: Int, resolutionNotes: String? = null): Result<AlertActionResponse> {
        return try {
            val response = api.resolveAlert(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                alertId,
                ResolveAlertRequest(resolutionNotes)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to resolve alert"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

