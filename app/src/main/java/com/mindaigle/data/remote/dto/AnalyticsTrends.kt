package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnalyticsTrendsResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("days")
    val days: Int,
    @SerializedName("stressTrends")
    val stressTrends: List<Float>,
    @SerializedName("activityTrends")
    val activityTrends: List<Float>,
    @SerializedName("dates")
    val dates: List<String>,
    @SerializedName("meta")
    val meta: AnalyticsTrendsMeta?,
    @SerializedName("startDate")
    val startDate: String? = null,
    @SerializedName("endDate")
    val endDate: String? = null,
    @SerializedName("peerComparison")
    val peerComparison: List<PeerComparisonItem>? = null,
    @SerializedName("studentTrends")
    val studentTrends: List<StudentTrendSeries>? = null
)

data class PeerComparisonItem(
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("studentName")
    val studentName: String,
    @SerializedName("lastStress")
    val lastStress: Int?,
    @SerializedName("lastMood")
    val lastMood: Int?,
    @SerializedName("checkinCount")
    val checkinCount: Int,
    @SerializedName("latestStressAny")
    val latestStressAny: Int? = null,
    @SerializedName("latestMoodAny")
    val latestMoodAny: Int? = null,
    @SerializedName("latestCheckinDate")
    val latestCheckinDate: String? = null
)

/** Per-student time series for peer comparison charts (one line per student). */
data class StudentTrendSeries(
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("studentName")
    val studentName: String,
    @SerializedName("stressByDay")
    val stressByDay: List<Float>,
    @SerializedName("activityByDay")
    val activityByDay: List<Float>
)

data class AnalyticsTrendsMeta(
    @SerializedName("stressDataPoints")
    val stressDataPoints: Int,
    @SerializedName("activityDataPoints")
    val activityDataPoints: Int
)
