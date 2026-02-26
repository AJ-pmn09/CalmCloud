package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

// Catalog
data class ScreenerCatalogItem(
    @SerializedName("screener_type") val screenerType: String,
    @SerializedName("name") val name: String,
    @SerializedName("questions") val questions: List<String>,
    @SerializedName("answer_scale") val answerScale: List<Int>
)

data class ScreenerCatalogResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<ScreenerCatalogItem>
)

// Instance
data class ScreenerInstance(
    @SerializedName("id") val id: Int,
    @SerializedName("student_id") val studentId: Int,
    @SerializedName("screener_type") val screenerType: String,
    @SerializedName("status") val status: String,
    @SerializedName("trigger_source") val triggerSource: String? = null,
    @SerializedName("assigned_by") val assignedBy: Int? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CreateScreenerInstanceRequest(
    @SerializedName("student_id") val studentId: Int,
    @SerializedName("screener_type") val screenerType: String,
    @SerializedName("trigger_source") val triggerSource: String? = "manual",
    @SerializedName("override_frequency") val overrideFrequency: Boolean? = null
)

data class ScreenerInstanceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: ScreenerInstance
)

data class ListScreenerInstancesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<ScreenerInstance>
)

data class GetScreenerInstanceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: ScreenerInstance
)

// Submit
data class ScreenerResponseItem(
    @SerializedName("question_index") val questionIndex: Int,
    @SerializedName("answer_value") val answerValue: Int
)

data class SubmitScreenerRequest(
    @SerializedName("student_id") val studentId: Int? = null,
    @SerializedName("responses") val responses: List<ScreenerResponseItem>
)

data class SubmitScreenerResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SubmitScreenerData
)

data class SubmitScreenerData(
    @SerializedName("instance_id") val instanceId: Int,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Int,
    @SerializedName("severity") val severity: String,
    @SerializedName("positive") val positive: Boolean
)

// Student report
data class ScreenerStudentReportResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: ScreenerStudentReportData
)

data class ScreenerStudentReportData(
    @SerializedName("studentId") val studentId: Int,
    @SerializedName("latestByType") val latestByType: Map<String, ScreenerInstanceWithScore>,
    @SerializedName("history") val history: List<ScreenerInstanceWithScore>
)

data class ScreenerInstanceWithScore(
    @SerializedName("id") val id: Int,
    @SerializedName("student_id") val studentId: Int,
    @SerializedName("screener_type") val screenerType: String,
    @SerializedName("status") val status: String,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("positive") val positive: Boolean? = null
)

// School report (optional)
data class ScreenerSchoolReportResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: ScreenerSchoolReportData
)

data class ScreenerSchoolReportData(
    @SerializedName("byType") val byType: Map<String, Map<String, Int>>
)
