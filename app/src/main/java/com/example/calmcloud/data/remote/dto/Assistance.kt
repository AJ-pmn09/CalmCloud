package com.example.calmcloud.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AssistanceRequest(
    @SerializedName("id")
    val id: Int,
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("studentName")
    val studentName: String?,
    @SerializedName("grade")
    val grade: Int?,
    @SerializedName("parentId")
    val parentId: Int,
    @SerializedName("parentName")
    val parentName: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("urgency")
    val urgency: String, // "low", "normal", "high"
    @SerializedName("status")
    val status: String, // "pending", "in-progress", "resolved"
    @SerializedName("handledBy")
    val handledBy: Int? = null,
    @SerializedName("handledByName")
    val handledByName: String? = null,
    @SerializedName("handledAt")
    val handledAt: String? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("createdAt")
    val createdAt: String
)

data class CreateAssistanceRequest(
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("urgency")
    val urgency: String
)

data class UpdateAssistanceRequest(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("notes")
    val notes: String? = null
)

data class AssistanceRequestsResponse(
    @SerializedName("requests")
    val requests: List<AssistanceRequest>
)

data class CreateAssistanceResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("requestId")
    val requestId: Int
)

data class UpdateAssistanceResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("request")
    val request: AssistanceRequest
)

