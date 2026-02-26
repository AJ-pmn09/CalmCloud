package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ActivityLog(
    @SerializedName("date")
    val date: String, // YYYY-MM-DD format
    @SerializedName("steps")
    val steps: Int,
    @SerializedName("sleep_hours")
    val sleepHours: Double,
    @SerializedName("hydration_percent")
    val hydrationPercent: Int,
    @SerializedName("nutrition_percent")
    val nutritionPercent: Int
)

data class ActivityLogsMeta(
    @SerializedName("requestedDays")
    val requestedDays: Int,
    @SerializedName("returnedCount")
    val returnedCount: Int,
    @SerializedName("totalInDb")
    val totalInDb: Int,
    @SerializedName("dateRange")
    val dateRange: DateRange?
)

data class DateRange(
    @SerializedName("earliest")
    val earliest: String?,
    @SerializedName("latest")
    val latest: String?
)

data class ActivityLogsResponse(
    @SerializedName("logs")
    val logs: List<ActivityLog>,
    @SerializedName("meta")
    val meta: ActivityLogsMeta?
)

/** Request body for POST /activity-logs/sync (wearable → backend). */
data class ActivitySyncRequest(
    @SerializedName("steps") val steps: Long? = null,
    @SerializedName("sleepMinutes") val sleepMinutes: Long? = null,
    @SerializedName("heartRate") val heartRate: Int? = null,
    @SerializedName("date") val date: String? = null // YYYY-MM-DD
)

/** Response from POST /activity-logs/sync */
data class ActivitySyncResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("date") val date: String? = null
)

// Computed achievements data (matches dashboard logic)
data class ComputedAchievement(
    val title: String,
    val description: String,
    val icon: String,
    val unlocked: Boolean,
    val progress: Int // 0-100
)

data class AchievementsData(
    val points: Int,
    val streak: Int,
    val achievements: List<ComputedAchievement>
)
