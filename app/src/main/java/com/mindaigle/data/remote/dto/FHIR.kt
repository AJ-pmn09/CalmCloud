package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

// FHIR Observation DTO (FHIR R4, HIPAA-aligned with optional meta.security)
data class FHIRObservation(
    @SerializedName("id")
    val id: String,
    @SerializedName("resourceType")
    val resourceType: String = "Observation",
    @SerializedName("status")
    val status: String = "final",
    @SerializedName("meta")
    val meta: FHIRMeta? = null,
    @SerializedName("code")
    val code: FHIRCode,
    @SerializedName("valueQuantity")
    val valueQuantity: FHIRValueQuantity? = null,
    @SerializedName("valueString")
    val valueString: String? = null,
    @SerializedName("effectiveDateTime")
    val effectiveDateTime: String,
    @SerializedName("subject")
    val subject: FHIRSubject
)

data class FHIRMeta(
    @SerializedName("security")
    val security: List<FHIRCoding>? = null
)

data class FHIRCode(
    @SerializedName("coding")
    val coding: List<FHIRCoding>
)

data class FHIRCoding(
    @SerializedName("system")
    val system: String = "http://loinc.org",
    @SerializedName("code")
    val code: String,
    @SerializedName("display")
    val display: String
)

data class FHIRValueQuantity(
    @SerializedName("value")
    val value: Double,
    @SerializedName("unit")
    val unit: String? = null
)

data class FHIRSubject(
    @SerializedName("reference")
    val reference: String
)

data class StudentFHIRData(
    @SerializedName("observations")
    val observations: List<FHIRObservation> = emptyList()
)

data class LatestCheckin(
    @SerializedName("stress_level")
    val stressLevel: Int?,
    @SerializedName("mood_rating")
    val moodRating: Int?,
    @SerializedName("mood")
    val mood: Int? = null, // Integer from database (not emotion string)
    @SerializedName("date")
    val date: String?,
    @SerializedName("stress_source")
    val stressSource: String? = null,
    @SerializedName("additional_notes")
    val additionalNotes: String? = null
)

data class ActivityData(
    @SerializedName("heart_rate")
    val heartRate: Int? = null,
    @SerializedName("steps")
    val steps: Int? = null,
    @SerializedName("sleep_hours")
    val sleepHours: Double? = null,
    @SerializedName("hydration_percent")
    val hydrationPercent: Int? = null,
    @SerializedName("nutrition_percent")
    val nutritionPercent: Int? = null,
    @SerializedName("mood")
    val mood: String? = null, // String from activity_logs
    @SerializedName("stress_level")
    val stressLevel: Int? = null
)

data class StudentDataResponse(
    @SerializedName("success")
    val success: Boolean? = null, // Optional - some endpoints include this
    @SerializedName("student")
    val student: StudentInfo,
    @SerializedName("fhirData")
    val fhirData: StudentFHIRData,
    @SerializedName("latestCheckin")
    val latestCheckin: LatestCheckin? = null,
    @SerializedName("activityData")
    val activityData: ActivityData? = null,
    @SerializedName("todayHydrationMl")
    val todayHydrationMl: Int? = null,
    @SerializedName("hydrationGoalMl")
    val hydrationGoalMl: Int? = null,
    @SerializedName("isParentView")
    val isParentView: Boolean = false
)

data class StudentInfo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName")
    val lastName: String? = null,
    @SerializedName("email")
    val email: String,
    @SerializedName("profilePictureUrl")
    val profilePictureUrl: String? = null
)

// Trends data for charts
data class TrendCheckin(
    @SerializedName("date")
    val date: String?,
    @SerializedName("stressLevel")
    val stressLevel: Int?,
    @SerializedName("moodRating")
    val moodRating: Int?,
    @SerializedName("mood")
    val mood: Int?,
    @SerializedName("stressSource")
    val stressSource: String?,
    @SerializedName("additionalNotes")
    val additionalNotes: String?
)

data class TrendActivity(
    @SerializedName("date")
    val date: String?,
    @SerializedName("heartRate")
    val heartRate: Int?,
    @SerializedName("steps")
    val steps: Int?,
    @SerializedName("sleepHours")
    val sleepHours: Double?,
    @SerializedName("hydrationPercent")
    val hydrationPercent: Int?,
    @SerializedName("nutritionPercent")
    val nutritionPercent: Int?,
    @SerializedName("mood")
    val mood: String?,
    @SerializedName("stressLevel")
    val stressLevel: Int?
)

data class TrendsDataResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("days")
    val days: Int,
    @SerializedName("checkins")
    val checkins: List<TrendCheckin>,
    @SerializedName("activities")
    val activities: List<TrendActivity>
)

data class FHIRExportResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("fhirBundle")
    val fhirBundle: String, // JSON string of FHIR Bundle
    @SerializedName("observationCount")
    val observationCount: Int
)
