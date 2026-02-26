package com.mindaigle.data.health

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

/**
 * Health Connect–compatible integration following the official architecture and codelab:
 * - developer.android.com/codelabs/health-connect
 * - developer.android.com/health-and-fitness/health-connect/architecture
 *
 * Architecture: SDK layer (this client) → Health Connect APK (permissions + data). We use
 * HealthConnectClient for IPC, request permissions via PermissionController, read with
 * aggregate() for cumulative data (steps), and write with insertRecords().
 */
object HealthConnectManager {

    /** Health Connect provider package (architecture: data management lives in this APK). */
    private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

    /**
     * Check availability (get-started / plan availability guide).
     * Returns SDK_AVAILABLE (3), SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED (2), or SDK_UNAVAILABLE (1).
     */
    fun getSdkStatus(context: Context): Int = try {
        HealthConnectClient.getSdkStatus(context.applicationContext, HEALTH_CONNECT_PACKAGE)
    } catch (e: Throwable) {
        try {
            HealthConnectClient.getSdkStatus(context.applicationContext)
        } catch (e2: Throwable) {
            1
        }
    }

    fun isAvailable(context: Context): Boolean = getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun isUpdateRequired(context: Context): Boolean =
        getSdkStatus(context) == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    /** Get or create the Health Connect client. Use application context. */
    fun getOrCreateClient(context: Context): HealthConnectClient? = try {
        HealthConnectClient.getOrCreate(context.applicationContext)
    } catch (e: Throwable) {
        null
    }

    /** Official permission contract — launch from an Activity to open the Health Connect permission screen. */
    fun createPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    /** Permission strings (read + write) so MindAigle appears in both "Can read" and "Can write" in Health Connect. */
    fun getRequiredPermissionStrings(): Set<String> = HealthConnectPermissions.readAndWritePermissionStringsFor(
        setOf(
            HealthDataType.STEPS,
            HealthDataType.SLEEP,
            HealthDataType.HEART_RATE,
            HealthDataType.CALORIES,
            HealthDataType.DISTANCE,
            HealthDataType.SPO2,
            HealthDataType.BLOOD_PRESSURE,
            HealthDataType.BODY_MASS
        )
    )

    /** Granted permission strings from the SDK (suspend). */
    suspend fun getGrantedPermissionStrings(context: Context): Set<String> = withContext(Dispatchers.IO) {
        val client = getOrCreateClient(context) ?: return@withContext emptySet()
        try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Throwable) {
            emptySet()
        }
    }

    /** Map granted permission strings to HealthDataType set. */
    fun grantedStringsToTypes(granted: Set<String>): Set<HealthDataType> =
        HealthConnectPermissions.grantedStringsToTypes(granted)

    /**
     * Read total steps in range (read-data guide: use aggregate() for cumulative types to avoid double-counting).
     * Reads data from all sources (e.g. Armitron Connect) that write to Health Connect.
     */
    suspend fun readStepsTotal(context: Context, start: Date, end: Date): Long? = withContext(Dispatchers.IO) {
        val client = getOrCreateClient(context) ?: return@withContext null
        try {
            val startInstant = start.toInstant()
            val endInstant = end.toInstant()
            val timeRange = TimeRangeFilter.between(startInstant, endInstant)
            val request = AggregateRequest(
                setOf(StepsRecord.COUNT_TOTAL),
                timeRange,
                emptySet()
            )
            val result: AggregationResult = client.aggregate(request)
            if (result.contains(StepsRecord.COUNT_TOTAL)) {
                result.get(StepsRecord.COUNT_TOTAL) ?: 0L
            } else null
        } catch (e: SecurityException) {
            Log.w("HealthConnectManager", "readStepsTotal: permission denied", e)
            null
        } catch (e: Throwable) {
            Log.w("HealthConnectManager", "readStepsTotal: ${e.message}", e)
            null
        }
    }

    /**
     * Write steps to Health Connect (write-data guide: insertRecords).
     * Use manualEntry() for app-originated data. Only write zero if device was worn and no activity (don't write zeros for missing/off-body).
     */
    suspend fun writeSteps(context: Context, count: Long, startTime: Instant, endTime: Instant): Boolean = withContext(Dispatchers.IO) {
        val client = getOrCreateClient(context) ?: return@withContext false
        try {
            val record = StepsRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.UTC,
                endTime = endTime,
                endZoneOffset = ZoneOffset.UTC,
                count = count,
                metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry()
            )
            client.insertRecords(listOf(record))
            true
        } catch (e: Throwable) {
            false
        }
    }

    /** Intent to open Health Connect app (add FLAG_ACTIVITY_NEW_TASK when starting from non-Activity). */
    fun getHealthConnectIntent(context: Context): Intent {
        val launch = context.packageManager.getLaunchIntentForPackage(HEALTH_CONNECT_PACKAGE)
        if (launch != null) return launch
        return try {
            HealthConnectClient.getHealthConnectManageDataIntent(context)
        } catch (e: Throwable) {
            Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE"))
        }
    }
}

private fun Date.toInstant(): java.time.Instant = java.time.Instant.ofEpochMilli(time)
