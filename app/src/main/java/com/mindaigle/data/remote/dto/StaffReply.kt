package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StaffReplyRequest(
    @SerializedName("replyMessage")
    val replyMessage: String,
    @SerializedName("note")
    val note: String? = null
)

data class StaffReplyResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("reply")
    val reply: Reply?,
    @SerializedName("noteCreated")
    val noteCreated: Boolean
)

data class Reply(
    @SerializedName("id")
    val id: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("createdAt")
    val createdAt: String
)

data class StudentRequest(
    @SerializedName("id")
    val id: Int,
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("studentName")
    val studentName: String,
    @SerializedName("studentEmail")
    val studentEmail: String,
    @SerializedName("studentGrade")
    val studentGrade: Int?,
    @SerializedName("subject")
    val subject: String?,
    @SerializedName("message")
    val message: String,
    @SerializedName("priority")
    val priority: String,
    @SerializedName("emergencyOverride")
    val emergencyOverride: Boolean,
    @SerializedName("createdAt")
    val createdAt: String
)

data class StudentRequestsResponse(
    @SerializedName("requests")
    val requests: List<StudentRequest>
)
