package com.mindaigle.data.health

import java.util.Date

/**
 * Cross-platform abstraction for reading health data from the device's health platform:
 * - Android: Google Health Connect (wearables sync via companion apps → Health Connect → our app)
 * - iOS: Apple HealthKit (wearables sync via companion apps → Apple Health → our app)
 *
 * We do NOT connect directly to wearable hardware. Data flows:
 * Armitron Connect watch → Armitron Connect app → Health Connect / HealthKit → MindAigle.
 */
interface HealthService {

    /** Whether the health platform is available (e.g. Health Connect installed). */
    suspend fun isAvailable(): Boolean

    /** Whether an update is required (e.g. Health Connect app update). */
    suspend fun isUpdateRequired(): Boolean

    /** Set of data types the user has granted read access to. */
    suspend fun getGrantedPermissions(): Set<HealthDataType>

    /** Request read permissions for the given types. Result delivered asynchronously via onResult (e.g. from system permission UI). */
    fun requestPermissions(types: Set<HealthDataType>, onResult: (Set<HealthDataType>) -> Unit)

    /** Steps in a time range (UTC). Use for time-series or daily totals. */
    suspend fun readSteps(start: Date, end: Date): List<HealthDataPoint>

    /** Aggregate total steps between start and end. */
    suspend fun readStepsTotal(start: Date, end: Date): Long?

    /** Heart rate samples in range. */
    suspend fun readHeartRate(start: Date, end: Date): List<HealthDataPoint>

    /** Active calories burned in range. */
    suspend fun readCaloriesBurned(start: Date, end: Date): List<HealthDataPoint>

    /** Total active calories in range. */
    suspend fun readCaloriesBurnedTotal(start: Date, end: Date): Double?

    /** Sleep sessions in range. */
    suspend fun readSleep(start: Date, end: Date): List<HealthDataPoint>

    /** Total sleep duration in minutes in range. */
    suspend fun readSleepTotalMinutes(start: Date, end: Date): Long?

    /** Walking/running distance in range. */
    suspend fun readDistance(start: Date, end: Date): List<HealthDataPoint>

    /** Oxygen saturation (SpO2) in range. */
    suspend fun readOxygenSaturation(start: Date, end: Date): List<HealthDataPoint>

    /** Body mass (weight) in range. */
    suspend fun readBodyMass(start: Date, end: Date): List<HealthDataPoint>

    /** Blood pressure readings in range (systolic/diastolic mmHg). */
    suspend fun readBloodPressure(start: Date, end: Date): List<BloodPressureReading>

    /** Last time we successfully synced; null if never. */
    suspend fun getLastSyncedAt(): Long?

    /** Mark sync completed at current time. */
    suspend fun setLastSyncedAt()
}
