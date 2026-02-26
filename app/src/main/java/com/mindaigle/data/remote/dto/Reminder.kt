package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ReminderConfig(
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("reminderEnabled")
    val reminderEnabled: Boolean,
    @SerializedName("reminderIntervalHours")
    val reminderIntervalHours: Int,
    @SerializedName("lastMissedCheckinAt")
    val lastMissedCheckinAt: String? = null,
    @SerializedName("lastReminderSentAt")
    val lastReminderSentAt: String? = null,
    @SerializedName("lastCheckinDate")
    val lastCheckinDate: String? = null
)

data class ReminderConfigResponse(
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("reminderEnabled")
    val reminderEnabled: Boolean,
    @SerializedName("reminderIntervalHours")
    val reminderIntervalHours: Int,
    @SerializedName("lastMissedCheckinAt")
    val lastMissedCheckinAt: String? = null,
    @SerializedName("lastReminderSentAt")
    val lastReminderSentAt: String? = null,
    @SerializedName("lastCheckinDate")
    val lastCheckinDate: String? = null,
    @SerializedName("smartScheduling")
    val smartScheduling: Boolean? = true,
    @SerializedName("quietHoursStart")
    val quietHoursStart: String? = "22:00:00",
    @SerializedName("quietHoursEnd")
    val quietHoursEnd: String? = "07:00:00",
    @SerializedName("maxPerDay")
    val maxPerDay: Int? = 3
)

data class UpdateReminderConfigRequest(
    @SerializedName("reminderEnabled")
    val reminderEnabled: Boolean? = null,
    @SerializedName("reminderIntervalHours")
    val reminderIntervalHours: Int? = null,
    @SerializedName("smartScheduling")
    val smartScheduling: Boolean? = null,
    @SerializedName("quietHoursStart")
    val quietHoursStart: String? = null,
    @SerializedName("quietHoursEnd")
    val quietHoursEnd: String? = null,
    @SerializedName("maxPerDay")
    val maxPerDay: Int? = null
)

data class UpdateReminderConfigResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("config")
    val config: ReminderConfigResponse
)

data class ReminderLog(
    @SerializedName("id")
    val id: Int,
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("studentName")
    val studentName: String,
    @SerializedName("grade")
    val grade: Int? = null,
    @SerializedName("reminderType")
    val reminderType: String,
    @SerializedName("escalationLevel")
    val escalationLevel: String,
    @SerializedName("reminderIntervalHours")
    val reminderIntervalHours: Int,
    @SerializedName("daysSinceCheckin")
    val daysSinceCheckin: Int? = null,
    @SerializedName("sentAt")
    val sentAt: String,
    @SerializedName("notificationMethod")
    val notificationMethod: String,
    @SerializedName("status")
    val status: String
)

data class ReminderLogsResponse(
    @SerializedName("logs")
    val logs: List<ReminderLog>
)

