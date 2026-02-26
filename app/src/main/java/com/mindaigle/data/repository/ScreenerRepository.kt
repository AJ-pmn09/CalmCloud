package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.*
import org.json.JSONObject

class ScreenerRepository {
    private fun errorMessageFromBody(errorBody: String?, fallback: String): String {
        if (!errorBody.isNullOrBlank()) {
            return try {
                JSONObject(errorBody).optString("error").takeIf { it.isNotEmpty() } ?: fallback
            } catch (_: Exception) {
                fallback
            }
        }
        return fallback
    }
    private val api = ApiClient.api

    suspend fun getCatalog(): Result<List<ScreenerCatalogItem>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockScreenerCatalogResponse().data)
        val token = AuthManager.getToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not logged in"))
        return try {
            val response = api.getScreenerCatalog("Bearer $token")
            when {
                response.code() == 404 || response.code() == 501 || response.code() == 401 -> Result.success(emptyList())
                response.isSuccessful && response.body() != null -> {
                    val body = response.body()!!
                    if (body.success) Result.success(body.data) else Result.failure(Exception("Catalog failed"))
                }
                else -> Result.failure(Exception(response.message() ?: "Failed to get catalog"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listMyInstances(status: String? = null): Result<List<ScreenerInstance>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockListScreenerInstancesResponse().data)
        val token = AuthManager.getToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not logged in"))
        return try {
            val response = api.listScreenerInstances(
                "Bearer $token",
                status = status,
                studentId = null
            )
            when {
                response.code() == 404 || response.code() == 501 || response.code() == 401 -> Result.success(emptyList())
                response.isSuccessful && response.body() != null -> {
                    val body = response.body()!!
                    if (body.success) Result.success(body.data) else Result.failure(Exception("List failed"))
                }
                else -> Result.failure(Exception(response.message() ?: "Failed to list instances"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInstance(instanceId: Int): Result<ScreenerInstance> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockScreenerInstance(instanceId, 0, "phq9", "assigned"))
        return try {
            val response = api.getScreenerInstance(
                "Bearer ${AuthManager.getToken()}",
                instanceId,
                studentId = null
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) Result.success(body.data) else Result.failure(Exception("Get instance failed"))
            } else Result.failure(Exception(response.message() ?: "Failed to get instance"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitScreener(instanceId: Int, studentId: Int?, answers: List<Int>): Result<SubmitScreenerData> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockSubmitScreenerData(instanceId, answers.sum(), "minimal", answers.sum() >= 10))
        return try {
            val responses = answers.mapIndexed { index, value ->
                ScreenerResponseItem(questionIndex = index, answerValue = value)
            }
            val response = api.submitScreener(
                "Bearer ${AuthManager.getToken()}",
                instanceId,
                SubmitScreenerRequest(studentId = studentId, responses = responses)
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) Result.success(body.data) else Result.failure(Exception("Submit failed"))
            } else Result.failure(Exception(response.message() ?: "Failed to submit"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInstance(studentId: Int, screenerType: String, overrideFrequency: Boolean = false): Result<ScreenerInstance> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockScreenerInstance(100 + studentId, studentId, screenerType, "assigned"))
        return try {
            val response = api.createScreenerInstance(
                "Bearer ${AuthManager.getToken()}",
                CreateScreenerInstanceRequest(
                    studentId = studentId,
                    screenerType = screenerType,
                    triggerSource = "manual",
                    overrideFrequency = if (overrideFrequency) true else null
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) Result.success(body.data) else Result.failure(Exception("Create failed"))
            } else {
                val rawBody = response.errorBody()?.string()
                val msg = errorMessageFromBody(rawBody, response.message() ?: "Failed to create instance")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listInstancesForStudent(studentId: Int, status: String? = null): Result<List<ScreenerInstance>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockListScreenerInstancesResponse().data)
        return try {
            val response = api.listScreenerInstances(
                "Bearer ${AuthManager.getToken()}",
                status = status,
                studentId = studentId
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) Result.success(body.data) else Result.failure(Exception("List failed"))
            } else Result.failure(Exception(response.message() ?: "Failed to list instances"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudentReport(studentId: Int): Result<ScreenerStudentReportData> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockScreenerStudentReportData(studentId))
        return try {
            val response = api.getScreenerStudentReport("Bearer ${AuthManager.getToken()}", studentId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) Result.success(body.data) else Result.failure(Exception("Report failed"))
            } else Result.failure(Exception(response.message() ?: "Failed to get report"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSchoolReport(): Result<ScreenerSchoolReportData> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockScreenerSchoolReportData())
        return try {
            val response = api.getScreenerSchoolReport("Bearer ${AuthManager.getToken()}")
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) Result.success(body.data) else Result.failure(Exception("Report failed"))
            } else Result.failure(Exception(response.message() ?: "Failed to get report"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
