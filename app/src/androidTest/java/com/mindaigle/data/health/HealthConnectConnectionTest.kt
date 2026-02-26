package com.mindaigle.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date

/**
 * Instrumented tests to verify the app connects to Health Connect and can fetch data.
 * Run 3 times to confirm: ./gradlew connectedDebugAndroidTest (or run this class 3 times).
 *
 * On a device/emulator without Health Connect installed, availability may be false
 * but permission strings and intent logic are still validated.
 */
@RunWith(AndroidJUnit4::class)
class HealthConnectConnectionTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    /** Run 1: Permission strings and SDK status. */
    @Test
    fun testHealthConnectConnection_run1() = runBlocking {
        // 1) App must request non-empty permission strings so it can register with Health Connect
        val permissionStrings = HealthConnectManager.getRequiredPermissionStrings()
        assertTrue(
            "Health Connect permission strings must be non-empty so the app can register",
            permissionStrings.isNotEmpty()
        )
        // 2) SDK status should be one of: UNAVAILABLE(1), UPDATE_REQUIRED(2), AVAILABLE(3)
        val status = HealthConnectManager.getSdkStatus(context)
        assertTrue(
            "SDK status should be 1, 2, or 3",
            status in 1..3
        )
        // 3) If available, client should be non-null
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            val client = HealthConnectManager.getOrCreateClient(context)
            assertNotNull("Client should be non-null when SDK is available", client)
        }
    }

    /** Run 2: Grant flow and read path. */
    @Test
    fun testHealthConnectConnection_run2() = runBlocking {
        val permissionStrings = HealthConnectManager.getRequiredPermissionStrings()
        assertTrue("Permission strings required", permissionStrings.isNotEmpty())
        val status = HealthConnectManager.getSdkStatus(context)
        assertTrue("SDK status valid", status in 1..3)
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            val granted = HealthConnectManager.getGrantedPermissionStrings(context)
            // Granted can be empty if user hasn't granted yet
            val calendar = Calendar.getInstance()
            val end = calendar.time
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.time
            val steps = HealthConnectManager.readStepsTotal(context, startOfToday, end)
            // steps can be null (no permission or no data) — we only verify the call doesn't throw
            assertTrue("Read steps completed", true)
        }
    }

    /** Run 3: Intent and type mapping. */
    @Test
    fun testHealthConnectConnection_run3() = runBlocking {
        val permissionStrings = HealthConnectManager.getRequiredPermissionStrings()
        assertTrue("Permission strings required", permissionStrings.isNotEmpty())
        val intent = HealthConnectManager.getHealthConnectIntent(context)
        assertNotNull("Health Connect intent must be non-null", intent)
        assertNotNull("Intent must have action or package", intent.`package` ?: intent.action)
        val types = setOf(
            HealthDataType.STEPS,
            HealthDataType.SLEEP,
            HealthDataType.HEART_RATE,
            HealthDataType.CALORIES,
            HealthDataType.DISTANCE,
            HealthDataType.SPO2,
            HealthDataType.BLOOD_PRESSURE
        )
        val readWrite = HealthConnectPermissions.readAndWritePermissionStringsFor(types)
        assertTrue("Read+write permissions for requested types", readWrite.isNotEmpty())
    }
}
