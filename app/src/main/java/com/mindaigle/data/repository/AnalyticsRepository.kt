package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.AnalyticsTrendsResponse

class AnalyticsRepository {
    private val api = ApiClient.api

    suspend fun getTrends(days: Int = 7, studentIds: List<Int>? = null): Result<AnalyticsTrendsResponse> {
        if (AuthManager.isTestMode()) return Result.success(TestMockData.mockAnalyticsTrendsResponse())
        return try {
            val studentIdsParam = studentIds?.takeIf { it.isNotEmpty() }?.joinToString(",") { it.toString() }
            val response = api.getAnalyticsTrends(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                days,
                studentIdsParam
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val stressNonZero = body.stressTrends.count { it > 0 }
                val activityNonZero = body.activityTrends.count { it > 0 }
                if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("AnalyticsRepository", "Trends loaded: ${body.stressTrends.size} stress points ($stressNonZero non-zero), ${body.activityTrends.size} activity points ($activityNonZero non-zero)")
                if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("AnalyticsRepository", "Meta: stressDataPoints=${body.meta?.stressDataPoints}, activityDataPoints=${body.meta?.activityDataPoints}")
                if (stressNonZero == 0 && activityNonZero == 0) {
                    android.util.Log.w("AnalyticsRepository", "No data found - check if students have completed check-ins")
                }
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("AnalyticsRepository", "Failed to get trends: ${response.code()} - ${response.message()}, Body: $errorBody")
                Result.failure(Exception(response.message() ?: "Failed to get trends data (${response.code()})"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsRepository", "Error getting trends", e)
            Result.failure(e)
        }
    }
}
