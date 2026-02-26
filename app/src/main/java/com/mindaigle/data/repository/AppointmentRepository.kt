package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.*

class AppointmentRepository {
    private val api = ApiClient.api

    private fun errorMessage(response: retrofit2.Response<*>): String {
        val body = response.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            return try {
                org.json.JSONObject(body).optString("error").takeIf { it.isNotEmpty() }
                    ?: (response.message() ?: "Failed to load appointments")
            } catch (_: Exception) {
                response.message() ?: "Failed to load appointments"
            }
        }
        return response.message() ?: "Failed to load appointments"
    }

    suspend fun getAppointments(): Result<List<Appointment>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockAppointmentsResponse().appointments)
        val token = AuthManager.getToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not logged in"))
        return try {
            val response = api.getAppointments("Bearer $token")
            when {
                response.code() == 404 || response.code() == 501 -> Result.success(emptyList())
                response.isSuccessful && response.body() != null -> Result.success(response.body()!!.appointments ?: emptyList())
                else -> Result.failure(Exception(errorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAppointment(request: CreateAppointmentRequest): Result<Appointment> {
        return try {
            val response = api.createAppointment(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                request
            )
            if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                Result.success(response.body()!!.appointment)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to create appointment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAppointment(id: Int, request: UpdateAppointmentRequest): Result<Appointment> {
        return try {
            val response = api.updateAppointment(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                id,
                request
            )
            if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                Result.success(response.body()!!.appointment)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update appointment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelAppointment(id: Int): Result<Boolean> {
        return try {
            val response = api.cancelAppointment(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                id
            )
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to cancel appointment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStaffAvailability(): Result<List<StaffMember>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockStaffList())
        val token = AuthManager.getToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not logged in"))
        return try {
            val response = api.getStaffAvailability("Bearer $token")
            when {
                response.code() == 404 || response.code() == 501 -> Result.success(emptyList())
                response.isSuccessful && response.body() != null -> Result.success(response.body()!!.staff ?: emptyList())
                else -> Result.failure(Exception(response.message() ?: "Failed to get staff availability"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
