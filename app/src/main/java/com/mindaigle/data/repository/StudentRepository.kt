package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.*

class StudentRepository {
    private val api = ApiClient.api

    suspend fun saveStudentData(
        studentId: Int?,
        fhirData: StudentFHIRData,
        additionalNotes: String? = null
    ): Result<SaveStudentDataResponse> {
        if (AuthManager.isTestMode()) {
            return Result.success(SaveStudentDataResponse(success = true, data = fhirData))
        }
        val token = AuthManager.getToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not logged in"))
        return try {
            val response = api.saveStudentData(
                "Bearer $token",
                SaveStudentDataRequest(studentId, fhirData, additionalNotes)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to save data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncActivityToBackend(steps: Long? = null, sleepMinutes: Long? = null, heartRate: Int? = null, date: String? = null): Result<ActivitySyncResponse> {
        if (AuthManager.isTestMode()) return Result.success(ActivitySyncResponse(success = true, date = date))
        val token = AuthManager.getToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not logged in"))
        if (steps == null && sleepMinutes == null && heartRate == null) return Result.failure(Exception("No data to sync"))
        return try {
            val response = api.syncActivityLogs(
                "Bearer $token",
                ActivitySyncRequest(steps = steps, sleepMinutes = sleepMinutes, heartRate = heartRate, date = date)
            )
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception(response.message() ?: "Sync failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyStudentData(): Result<StudentDataResponse> {
        if (AuthManager.isTestMode()) {
            val email = AuthManager.getTestUserEmail() ?: "student@test.mindaigle"
            val name = AuthManager.getTestUserName() ?: "Test Student"
            return Result.success(TestMockData.mockStudentDataResponse(email, name))
        }
        val token = AuthManager.getToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not logged in"))
        return try {
            val response = api.getMyStudentData(
                "Bearer $token"
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudentData(studentId: Int): Result<StudentDataResponse> {
        if (AuthManager.isTestMode()) {
            val email = AuthManager.getTestUserEmail() ?: "student@test.mindaigle"
            val name = AuthManager.getTestUserName() ?: "Test Student"
            return Result.success(TestMockData.mockStudentDataResponse(email, name))
        }
        return try {
            val response = api.getStudentData(
                "Bearer ${AuthManager.getToken()}",
                studentId
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllStudents(): Result<List<Student>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockStudentsList())
        return try {
            val response = api.getStudents(
                "Bearer ${AuthManager.getToken()}"
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.students)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get students"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChildren(): Result<List<Child>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockChildrenList())
        return try {
            val response = api.getChildren(
                "Bearer ${AuthManager.getToken()}"
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.children)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get children"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendsData(days: Int = 7): Result<TrendsDataResponse> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockTrendsDataResponse())
        val token = AuthManager.getToken()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not logged in"))
        return try {
            val response = api.getTrendsData(
                "Bearer $token",
                days
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get trends data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportFHIRData(startDate: String, endDate: String): Result<String> {
        if (AuthManager.isTestMode()) return Result.success("{}")
        return try {
            val response = api.exportFHIRData(
                "Bearer ${AuthManager.getToken()}",
                startDate,
                endDate
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.fhirBundle)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to export FHIR data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Export a student's data as FHIR (for associates/experts). */
    suspend fun exportFHIRDataForStudent(studentId: Int, startDate: String, endDate: String): Result<String> {
        if (AuthManager.isTestMode()) return Result.success("{}")
        return try {
            val response = api.exportFHIRDataForStudent(
                "Bearer ${AuthManager.getToken()}",
                studentId,
                startDate,
                endDate
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.fhirBundle)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to export FHIR data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

