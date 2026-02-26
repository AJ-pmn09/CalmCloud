package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.*

class CounselorNoteRepository {
    private val api = ApiClient.api

    suspend fun getCounselorNotes(): Result<List<CounselorNote>> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockCounselorNotesResponse().notes)
        return try {
            val response = api.getCounselorNotes(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}"
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.notes)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get counselor notes"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
