package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SendMessageRequest(
    @SerializedName("recipientType")
    val recipientType: String, // "student", "cohort", "all"
    @SerializedName("recipientId")
    val recipientId: Int? = null,
    @SerializedName("recipientCohort")
    val recipientCohort: String? = null, // "grade_9", "school_1", "all_students"
    @SerializedName("subject")
    val subject: String? = null,
    @SerializedName("message")
    val message: String,
    @SerializedName("priority")
    val priority: String? = "normal", // "low", "normal", "high", "urgent"
    @SerializedName("parentVisible")
    val parentVisible: Boolean? = true,
    @SerializedName("emergencyOverride")
    val emergencyOverride: Boolean? = false
)

data class SendMessageResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("communicationIds")
    val communicationIds: List<Int>,
    @SerializedName("recipientCount")
    val recipientCount: Int
)

data class Communication(
    @SerializedName("id")
    val id: Int,
    @SerializedName("senderId")
    val senderId: Int? = null,
    @SerializedName("senderName")
    val senderName: String,
    @SerializedName("senderEmail")
    val senderEmail: String? = null,
    @SerializedName("senderRole")
    val senderRole: String,
    @SerializedName("recipientType")
    val recipientType: String,
    @SerializedName("subject")
    val subject: String? = null,
    @SerializedName("message")
    val message: String,
    @SerializedName("priority")
    val priority: String,
    @SerializedName("parentVisible")
    val parentVisible: Boolean,
    @SerializedName("emergencyOverride")
    val emergencyOverride: Boolean,
    @SerializedName("status")
    val status: String,
    @SerializedName("readAt")
    val readAt: String? = null,
    @SerializedName("createdAt")
    val createdAt: String
)

data class CommunicationsResponse(
    @SerializedName("communications")
    val communications: List<Communication>
)

data class MessageActionResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: Communication
)

data class SentMessage(
    @SerializedName("id")
    val id: Int,
    @SerializedName("recipientType")
    val recipientType: String,
    @SerializedName("recipientId")
    val recipientId: Int? = null,
    @SerializedName("recipientName")
    val recipientName: String? = null,
    @SerializedName("recipientCohort")
    val recipientCohort: String? = null,
    @SerializedName("subject")
    val subject: String? = null,
    @SerializedName("message")
    val message: String,
    @SerializedName("priority")
    val priority: String,
    @SerializedName("parentVisible")
    val parentVisible: Boolean,
    @SerializedName("emergencyOverride")
    val emergencyOverride: Boolean,
    @SerializedName("status")
    val status: String,
    @SerializedName("createdAt")
    val createdAt: String
)

data class SentMessagesResponse(
    @SerializedName("communications")
    val communications: List<SentMessage>,
    @SerializedName("total")
    val total: Int
)

