package com.example.calmcloud.data.repository

import com.example.calmcloud.data.remote.ApiClient
import com.example.calmcloud.data.remote.dto.*

class StudentRepository {
    private val api = ApiClient.api

    suspend fun saveStudentData(
        studentId: Int?,
        fhirData: StudentFHIRData
    ): Result<SaveStudentDataResponse> {
        return try {
            val response = api.saveStudentData(
                "Bearer ${com.example.calmcloud.data.remote.AuthManager.getToken()}",
                SaveStudentDataRequest(studentId, fhirData)
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

    suspend fun getStudentData(studentId: Int): Result<StudentDataResponse> {
        return try {
            val response = api.getStudentData(
                "Bearer ${com.example.calmcloud.data.remote.AuthManager.getToken()}",
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
        return try {
            val response = api.getStudents(
                "Bearer ${com.example.calmcloud.data.remote.AuthManager.getToken()}"
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
        return try {
            val response = api.getChildren(
                "Bearer ${com.example.calmcloud.data.remote.AuthManager.getToken()}"
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
}

