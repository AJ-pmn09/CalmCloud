package com.mindaigle.data.health

import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord

/**
 * Direct use of Health Connect SDK for permission request so the app is registered
 * with Health Connect and appears under "Can read steps" (and other types). Using the
 * official contract and permission strings ensures the system shows our app in the
 * access list when the user grants access.
 */
object HealthConnectPermissions {

    private val TYPE_TO_READ_PERMISSION: Map<HealthDataType, String> = mapOf(
        HealthDataType.STEPS to HealthPermission.getReadPermission(StepsRecord::class),
        HealthDataType.SLEEP to HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthDataType.HEART_RATE to HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthDataType.CALORIES to HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthDataType.DISTANCE to HealthPermission.getReadPermission(DistanceRecord::class),
        HealthDataType.SPO2 to HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthDataType.BODY_MASS to HealthPermission.getReadPermission(WeightRecord::class),
        HealthDataType.BLOOD_PRESSURE to HealthPermission.getReadPermission(BloodPressureRecord::class)
    )

    private val TYPE_TO_WRITE_PERMISSION: Map<HealthDataType, String> = mapOf(
        HealthDataType.STEPS to HealthPermission.getWritePermission(StepsRecord::class),
        HealthDataType.SLEEP to HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthDataType.CALORIES to HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthDataType.DISTANCE to HealthPermission.getWritePermission(DistanceRecord::class)
    )

    /** Permission strings to request for the given types (read only). */
    fun permissionStringsFor(types: Set<HealthDataType>): Set<String> =
        types.mapNotNull { TYPE_TO_READ_PERMISSION[it] }.toSet()

    /** Read + write permission strings so our app appears in both "Can read" and "Can write" in Health Connect. */
    fun readAndWritePermissionStringsFor(types: Set<HealthDataType>): Set<String> {
        val read = types.mapNotNull { TYPE_TO_READ_PERMISSION[it] }
        val write = types.mapNotNull { TYPE_TO_WRITE_PERMISSION[it] }
        return (read + write).toSet()
    }

    /** Map granted permission strings back to HealthDataType. */
    fun grantedStringsToTypes(granted: Set<String>): Set<HealthDataType> =
        TYPE_TO_READ_PERMISSION.entries
            .filter { (_, perm) -> granted.any { g -> g == perm } }
            .map { it.key }
            .toSet()

    /** Creates the official permission contract so launching it registers our app with Health Connect. */
    fun createPermissionContract(): ActivityResultContract<Set<String>, Set<String>>? =
        try {
            PermissionController.createRequestPermissionResultContract()
        } catch (e: Throwable) {
            null
        }
}
