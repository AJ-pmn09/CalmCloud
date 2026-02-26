package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmergencyAlert(
    @SerializedName("id")
    val id: Int,
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("studentName")
    val studentName: String? = null,
    @SerializedName("studentEmail")
    val studentEmail: String? = null,
    @SerializedName("grade")
    val grade: Int? = null,
    @SerializedName("alertType")
    val alertType: String, // "emergency", "urgent", "support"
    @SerializedName("status")
    val status: String, // "active", "acknowledged", "resolved", "cancelled"
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("acknowledgedBy")
    val acknowledgedBy: Int? = null,
    @SerializedName("acknowledgedByName")
    val acknowledgedByName: String? = null,
    @SerializedName("acknowledgedAt")
    val acknowledgedAt: String? = null,
    @SerializedName("resolvedBy")
    val resolvedBy: Int? = null,
    @SerializedName("resolvedByName")
    val resolvedByName: String? = null,
    @SerializedName("resolvedAt")
    val resolvedAt: String? = null,
    @SerializedName("resolutionNotes")
    val resolutionNotes: String? = null
)

data class SuicideRiskScreeningQuestion(
    @SerializedName("question")
    val question: String,
    @SerializedName("response")
    val response: String // "yes", "no", "sometimes", "high", "moderate", "low"
)

data class SuicideRiskScreening(
    @SerializedName("questions")
    val questions: List<SuicideRiskScreeningQuestion>
)

data class CreateEmergencyAlertRequest(
    @SerializedName("alertType")
    val alertType: String? = "emergency", // "emergency", "urgent", "support"
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("suicideRiskScreening")
    val suicideRiskScreening: SuicideRiskScreening? = null
)

data class SuicideRiskScreeningResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("studentName")
    val studentName: String,
    @SerializedName("emergencyAlertId")
    val emergencyAlertId: Int,
    @SerializedName("screeningQuestions")
    val screeningQuestions: Map<String, Any>, // JSON object
    @SerializedName("riskScore")
    val riskScore: Int,
    @SerializedName("riskLevel")
    val riskLevel: String, // "low", "moderate", "high", "critical"
    @SerializedName("immediateActionRequired")
    val immediateActionRequired: Boolean,
    @SerializedName("screeningCompletedAt")
    val screeningCompletedAt: String
)

data class CreateEmergencyAlertResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("alert")
    val alert: EmergencyAlert
)

data class EmergencyAlertsResponse(
    @SerializedName("alerts")
    val alerts: List<EmergencyAlert>
)

data class ResolveAlertRequest(
    @SerializedName("resolutionNotes")
    val resolutionNotes: String? = null
)

data class AlertActionResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("alert")
    val alert: EmergencyAlert
)

