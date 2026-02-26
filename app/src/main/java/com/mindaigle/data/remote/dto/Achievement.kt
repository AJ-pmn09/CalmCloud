package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class Achievement(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("icon")
    val icon: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("points")
    val points: Int,
    @SerializedName("unlocked")
    val unlocked: Boolean = false,
    @SerializedName("unlockedAt")
    val unlockedAt: String? = null,
    @SerializedName("progress")
    val progress: Int = 0
)

data class AchievementsResponse(
    @SerializedName("achievements")
    val achievements: List<Achievement>
)

data class UnlockAchievementRequest(
    @SerializedName("achievementId")
    val achievementId: Int
)

data class UnlockAchievementResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("achievement")
    val achievement: Achievement
)
