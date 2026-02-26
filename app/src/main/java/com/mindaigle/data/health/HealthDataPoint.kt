package com.mindaigle.data.health

import java.util.Date

/**
 * Normalized health data point — same model for Android (Health Connect) and future iOS (HealthKit).
 * Timestamps are UTC; store original timezone separately if needed.
 */
data class HealthDataPoint(
    val type: HealthDataType,
    val value: Double,
    val unit: String,
    val startDate: Date,
    val endDate: Date,
    val sourceName: String,
    val sourceId: String
)

enum class HealthDataType {
    STEPS,
    HEART_RATE,
    CALORIES,
    SLEEP,
    DISTANCE,
    SPO2,
    BODY_MASS,
    BLOOD_PRESSURE
}

/** Single blood pressure reading (systolic/diastolic in mmHg). */
data class BloodPressureReading(
    val systolic: Double,
    val diastolic: Double,
    val date: java.util.Date
)

/** Summary for a single day or range (e.g. total steps today). */
data class HealthSummary(
    val type: HealthDataType,
    val value: Double,
    val unit: String,
    val startDate: Date,
    val endDate: Date,
    val sourceName: String? = null
)
