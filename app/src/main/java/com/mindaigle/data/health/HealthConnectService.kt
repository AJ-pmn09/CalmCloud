package com.mindaigle.data.health

import android.content.Context
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Android implementation of [HealthService] using Google Health Connect.
 * Does NOT use Google Fit (deprecated). Wearables sync via companion apps → Health Connect → our app.
 */
class HealthConnectService(private val context: Context) : HealthService {

    private val cache = HealthDataCache(context)

    private val client: Any? by lazy {
        try {
            val clazz = Class.forName("androidx.health.connect.client.HealthConnectClient")
            val getOrCreate = clazz.getMethod("getOrCreate", Context::class.java)
            getOrCreate.invoke(null, context.applicationContext)
        } catch (e: Throwable) {
            null
        }
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        HealthConnectManager.isAvailable(context) || reflectionIsAvailable()
    }

    private fun reflectionIsAvailable(): Boolean = try {
        val clazz = Class.forName("androidx.health.connect.client.HealthConnectClient")
        val getSdkStatus = clazz.getMethod("getSdkStatus", Context::class.java, String::class.java)
        val status = getSdkStatus.invoke(null, context.applicationContext, HEALTH_CONNECT_PACKAGE) as? Int ?: 1
        status == 3
    } catch (e: Throwable) {
        false
    }

    override suspend fun isUpdateRequired(): Boolean = withContext(Dispatchers.IO) {
        HealthConnectManager.isUpdateRequired(context)
    }

    override suspend fun getGrantedPermissions(): Set<HealthDataType> = withContext(Dispatchers.IO) {
        val fromManager = HealthConnectManager.getGrantedPermissionStrings(context)
        if (fromManager.isNotEmpty()) return@withContext HealthConnectManager.grantedStringsToTypes(fromManager)
        reflectionGetGrantedPermissions()
    }

    private suspend fun reflectionGetGrantedPermissions(): Set<HealthDataType> = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext emptySet()
        try {
            val permController = c.javaClass.getMethod("getPermissionController").invoke(c) ?: return@withContext emptySet()
            val grantedStrings = suspendCoroutine { cont ->
                val getGranted = permController.javaClass.methods
                    .firstOrNull { it.name == "getGrantedPermissions" && it.parameterCount == 1 && it.parameterTypes[0].name.contains("Continuation") }
                    ?: return@suspendCoroutine cont.resume(emptySet())
                getGranted.invoke(permController, object : Continuation<Any?> {
                    override val context = EmptyCoroutineContext
                    override fun resumeWith(result: kotlin.Result<Any?>) {
                        result.fold(
                            onSuccess = { value ->
                                @Suppress("UNCHECKED_CAST")
                                val set = value as? Set<*> ?: emptySet<Any>()
                                cont.resume(set.mapNotNull { g -> g?.toString() }.toSet())
                            },
                            onFailure = { cont.resume(emptySet()) }
                        )
                    }
                })
            }
            val upper = grantedStrings.map { it.uppercase() }.toSet()
            HEALTH_TYPE_TO_RECORD_CLASS.keys.filter { type ->
                upper.any { s -> s.contains(permissionSuffix(type)) }
            }.toSet()
        } catch (e: Throwable) {
            emptySet()
        }
    }

    private fun permissionSuffix(type: HealthDataType): String = when (type) {
        HealthDataType.STEPS -> "READ_STEPS"
        HealthDataType.SLEEP -> "READ_SLEEP"
        HealthDataType.HEART_RATE -> "READ_HEART_RATE"
        HealthDataType.CALORIES -> "READ_ACTIVE_CALORIES"
        HealthDataType.DISTANCE -> "READ_DISTANCE"
        HealthDataType.SPO2 -> "READ_OXYGEN"
        HealthDataType.BODY_MASS -> "READ_WEIGHT"
        HealthDataType.BLOOD_PRESSURE -> "READ_BLOOD_PRESSURE"
    }

    override fun requestPermissions(types: Set<HealthDataType>, onResult: (Set<HealthDataType>) -> Unit) {
        // Launcher must be created in UI; UI calls parseGrantedAndInvoke when result is received.
        // This implementation cannot launch the permission UI from here; the UI layer does that.
        // So we only need to expose getContract() and getPermissionObjects(types) for the UI.
        onResult(emptySet())
    }

    /**
     * Returns (contract, permission set) for the given types, or null if client unavailable.
     * Pass optional [callerContext] (e.g. Activity from LocalContext.current) so the client
     * is created with that context—required on some devices for the permission UI to work.
     */
    fun getContractAndPermissions(types: Set<HealthDataType>, callerContext: Context? = null): Pair<Any?, Set<*>>? {
        val c = when {
            callerContext != null -> try {
                val clazz = Class.forName("androidx.health.connect.client.HealthConnectClient")
                val getOrCreate = clazz.getMethod("getOrCreate", Context::class.java)
                getOrCreate.invoke(null, callerContext)
            } catch (e: Throwable) {
                null
            }
            else -> client
        } ?: return null
        return try {
            val permController = (c as Any).javaClass.getMethod("getPermissionController").invoke(c) ?: return null
            val contractMethod = listOf(
                "createRequestPermissionResultContract",
                "requestPermissionResultContract",
                "createRequestPermissionContract"
            ).firstNotNullOfOrNull { name ->
                try {
                    permController.javaClass.getMethod(name)
                } catch (_: Throwable) {
                    null
                }
            } ?: return null
            val contract = contractMethod.invoke(permController) ?: return null
            val perms = types.mapNotNull { getReadPermissionObject(it) }.toSet()
            if (perms.isEmpty()) null else Pair(contract, perms)
        } catch (e: Throwable) {
            null
        }
    }

    /** Map granted permission (from launcher result) back to HealthDataType. */
    fun parseGrantedToTypes(granted: Set<*>): Set<HealthDataType> {
        return HEALTH_TYPE_TO_RECORD_CLASS.keys.filter { type ->
            val permObj = getReadPermissionObject(type) ?: return@filter false
            granted.any { g -> g == permObj || (g != null && g.toString() == permObj.toString()) }
        }.toSet()
    }

    /** Returns the Health Connect Permission object for read access to this type (required for the contract). */
    private fun getReadPermissionObject(type: HealthDataType): Any? = try {
        val recordClass = HEALTH_TYPE_TO_RECORD_CLASS[type] ?: return null
        val permClass = Class.forName("androidx.health.connect.client.permission.HealthPermission")
        val getRead = permClass.methods.firstOrNull { m ->
            m.parameterCount == 1 && m.name.lowercase().contains("read") && m.name.lowercase().contains("permission") &&
            (m.parameterTypes[0] == Class::class.java || m.parameterTypes[0].name == "kotlin.reflect.KClass")
        } ?: permClass.getMethod("getReadPermission", Class::class.java)
        getRead.invoke(null, recordClass)
    } catch (e: Throwable) {
        null
    }

    private fun getReadPermissionString(type: HealthDataType): String? = try {
        val obj = getReadPermissionObject(type) ?: return null
        obj.toString()
    } catch (e: Throwable) {
        null
    }

    override suspend fun readSteps(start: Date, end: Date): List<HealthDataPoint> = withContext(Dispatchers.IO) {
        // Health Connect prefers aggregate for steps; we have readStepsTotal for totals.
        emptyList()
    }

    override suspend fun readStepsTotal(start: Date, end: Date): Long? = withContext(Dispatchers.IO) {
        HealthConnectManager.readStepsTotal(context, start, end) ?: reflectionReadStepsTotal(start, end)
    }

    private suspend fun reflectionReadStepsTotal(start: Date, end: Date): Long? = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext null
        try {
            val startInstant = start.toInstant()
            val endInstant = end.toInstant()
            val stepsRecordClass = Class.forName("androidx.health.connect.client.records.StepsRecord")
            val timeRangeFilterClass = Class.forName("androidx.health.connect.client.time.TimeRangeFilter")
            val between = timeRangeFilterClass.getMethod("between", java.time.Instant::class.java, java.time.Instant::class.java)
            val timeRange = between.invoke(null, startInstant, endInstant)
            val aggregateResult = try {
                val countTotal = listOf("COUNT_TOTAL", "STEPS_COUNT_TOTAL").firstNotNullOfOrNull { name ->
                    try { stepsRecordClass.getField(name).get(null) } catch (_: Throwable) { null }
                }
                if (countTotal != null) {
                    val builderClass = Class.forName("androidx.health.connect.client.aggregate.AggregateRequest\$Builder")
                    val builder = builderClass.getConstructor().newInstance()
                    builderClass.getMethod("addDataType", Class::class.java).invoke(builder, stepsRecordClass)
                    builderClass.getMethod("setTimeRangeFilter", timeRangeFilterClass).invoke(builder, timeRange)
                    val request = builderClass.getMethod("build").invoke(builder)
                    val aggregateMethod = c.javaClass.getMethod("aggregate", request!!.javaClass)
                    val result = aggregateMethod.invoke(c, request) as? Map<*, *>
                    result?.get(countTotal) as? Long
                } else null
            } catch (_: Throwable) { null }
            if (aggregateResult != null) return@withContext aggregateResult
            readStepsTotalFromRecords(c, stepsRecordClass, timeRangeFilterClass, timeRange as Any, startInstant, endInstant)
        } catch (e: Throwable) {
            null
        }
    }

    private suspend fun readStepsTotalFromRecords(
        c: Any,
        stepsRecordClass: Class<*>,
        timeRangeFilterClass: Class<*>,
        timeRange: Any,
        startInstant: java.time.Instant,
        endInstant: java.time.Instant
    ): Long? = suspendCoroutine { cont ->
        try {
            val requestBuilderClass = Class.forName("androidx.health.connect.client.request.ReadRecordsRequestUsingFilters\$Builder")
            val builder = requestBuilderClass.getConstructor(Class::class.java).newInstance(stepsRecordClass)
            builder.javaClass.getMethod("setTimeRangeFilter", timeRangeFilterClass).invoke(builder, timeRange)
            val request = builder.javaClass.getMethod("build").invoke(builder) ?: run { cont.resume(null); return@suspendCoroutine }
            val continuationClass = Class.forName("kotlin.coroutines.Continuation")
            val method = c.javaClass.methods.firstOrNull { it.name == "readRecords" && it.parameterCount == 2 && it.parameterTypes[1].name.contains("Continuation") }
                ?: c.javaClass.getMethod("readRecords", request.javaClass, continuationClass)
            method.invoke(c, request, object : Continuation<Any?> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: kotlin.Result<Any?>) {
                    result.fold(
                        onSuccess = { response ->
                            val records = (response?.javaClass?.getMethod("getRecords")?.invoke(response)
                                ?: response?.javaClass?.getMethod("records")?.invoke(response)) as? List<*> ?: emptyList<Any>()
                            var total = 0L
                            for (record in records) {
                                if (record == null) continue
                                val count = try {
                                    (record.javaClass.getMethod("getCount").invoke(record) as? Number)?.toLong() ?: 0L
                                } catch (_: Throwable) {
                                    try { (record.javaClass.getField("count").get(record) as? Number)?.toLong() ?: 0L } catch (_: Throwable) { 0L }
                                }
                                total += count
                            }
                            cont.resume(if (total == 0L && records.isEmpty()) null else total)
                        },
                        onFailure = { cont.resumeWithException(it) }
                    )
                }
            })
        } catch (e: Throwable) {
            cont.resume(null)
        }
    }

    override suspend fun readHeartRate(start: Date, end: Date): List<HealthDataPoint> = withContext(Dispatchers.IO) {
        readSamples(
            recordClass = Class.forName("androidx.health.connect.client.records.HeartRateRecord"),
            start = start,
            end = end,
            type = HealthDataType.HEART_RATE,
            valueGetter = { r -> (r.javaClass.getMethod("getSamples").invoke(r) as? List<*>)?.firstOrNull()?.let { s -> (s?.javaClass?.getMethod("getBeatsPerMinute")?.invoke(s) as? Number)?.toDouble() ?: 0.0 } ?: 0.0 },
            unit = "bpm"
        )
    }

    override suspend fun readCaloriesBurned(start: Date, end: Date): List<HealthDataPoint> = withContext(Dispatchers.IO) {
        readSamples(
            recordClass = Class.forName("androidx.health.connect.client.records.ActiveCaloriesBurnedRecord"),
            start = start,
            end = end,
            type = HealthDataType.CALORIES,
            valueGetter = { r -> (r.javaClass.getMethod("getEnergy").invoke(r) as? Number)?.toDouble() ?: 0.0 },
            unit = "kcal"
        )
    }

    override suspend fun readCaloriesBurnedTotal(start: Date, end: Date): Double? = withContext(Dispatchers.IO) {
        val list = readCaloriesBurned(start, end)
        if (list.isEmpty()) null else list.sumOf { it.value }
    }

    /** Invokes client.readRecords(request) as a suspend call (passes Continuation). Returns records list or null. */
    private suspend fun readRecordsList(c: Any, request: Any): List<*>? = suspendCoroutine { cont ->
        try {
            val continuationClass = Class.forName("kotlin.coroutines.Continuation")
            val method = c.javaClass.methods.firstOrNull { it.name == "readRecords" && it.parameterCount == 2 && it.parameterTypes.getOrNull(1)?.name?.contains("Continuation") == true }
                ?: c.javaClass.getMethod("readRecords", request.javaClass, continuationClass)
            method.invoke(c, request, object : Continuation<Any?> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: kotlin.Result<Any?>) {
                    result.fold(
                        onSuccess = { response ->
                            val list = (response?.javaClass?.getMethod("getRecords")?.invoke(response)
                                ?: response?.javaClass?.getMethod("records")?.invoke(response)) as? List<*> ?: emptyList<Any>()
                            cont.resume(list)
                        },
                        onFailure = { cont.resumeWithException(it) }
                    )
                }
            })
        } catch (e: Throwable) {
            cont.resume(null)
        }
    }

    override suspend fun readSleep(start: Date, end: Date): List<HealthDataPoint> = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext emptyList()
        try {
            val sleepClass = Class.forName("androidx.health.connect.client.records.SleepSessionRecord")
            val timeRangeFilterClass = Class.forName("androidx.health.connect.client.time.TimeRangeFilter")
            val between = timeRangeFilterClass.getMethod("between", java.time.Instant::class.java, java.time.Instant::class.java)
            val startInstant = start.toInstant()
            val endInstant = end.toInstant()
            val timeRange = between.invoke(null, startInstant, endInstant)
            val requestBuilderClass = Class.forName("androidx.health.connect.client.request.ReadRecordsRequestUsingFilters\$Builder")
            val builder = requestBuilderClass.getConstructor(Class::class.java).newInstance(sleepClass)
            builder.javaClass.getMethod("setTimeRangeFilter", timeRangeFilterClass).invoke(builder, timeRange)
            val request = builder.javaClass.getMethod("build").invoke(builder) ?: return@withContext emptyList()
            val records = readRecordsList(c, request) ?: return@withContext emptyList()
            records.mapNotNull { record ->
                try {
                    val startTime = record?.javaClass?.getMethod("getStartTime")?.invoke(record) as? java.time.Instant
                    val endTime = record?.javaClass?.getMethod("getEndTime")?.invoke(record) as? java.time.Instant
                    val source = record?.javaClass?.getMethod("getMetadata")?.invoke(record)?.let { m -> m?.javaClass?.getMethod("getDataOrigin")?.invoke(m)?.toString() } ?: "Health Connect"
                    if (startTime != null && endTime != null) {
                        val minutes = java.time.Duration.between(startTime, endTime).toMinutes()
                        HealthDataPoint(
                            type = HealthDataType.SLEEP,
                            value = minutes.toDouble(),
                            unit = "min",
                            startDate = Date.from(startTime),
                            endDate = Date.from(endTime),
                            sourceName = source,
                            sourceId = source
                        )
                    } else null
                } catch (_: Throwable) { null }
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }

    override suspend fun readSleepTotalMinutes(start: Date, end: Date): Long? = withContext(Dispatchers.IO) {
        val sessions = readSleep(start, end)
        if (sessions.isEmpty()) null else sessions.sumOf { it.value.toLong() }
    }

    override suspend fun readDistance(start: Date, end: Date): List<HealthDataPoint> = withContext(Dispatchers.IO) {
        readSamples(
            recordClass = Class.forName("androidx.health.connect.client.records.DistanceRecord"),
            start = start,
            end = end,
            type = HealthDataType.DISTANCE,
            valueGetter = { r -> (r.javaClass.getMethod("getDistance").invoke(r) as? Number)?.toDouble() ?: 0.0 },
            unit = "m"
        )
    }

    override suspend fun readOxygenSaturation(start: Date, end: Date): List<HealthDataPoint> = withContext(Dispatchers.IO) {
        readSamples(
            recordClass = Class.forName("androidx.health.connect.client.records.OxygenSaturationRecord"),
            start = start,
            end = end,
            type = HealthDataType.SPO2,
            valueGetter = { r -> (r.javaClass.getMethod("getPercentage").invoke(r) as? Number)?.toDouble() ?: 0.0 },
            unit = "%"
        )
    }

    override suspend fun readBodyMass(start: Date, end: Date): List<HealthDataPoint> = withContext(Dispatchers.IO) {
        readSamples(
            recordClass = Class.forName("androidx.health.connect.client.records.WeightRecord"),
            start = start,
            end = end,
            type = HealthDataType.BODY_MASS,
            valueGetter = { r -> (r.javaClass.getMethod("getWeight").invoke(r) as? Number)?.toDouble() ?: 0.0 },
            unit = "kg"
        )
    }

    override suspend fun readBloodPressure(start: Date, end: Date): List<BloodPressureReading> = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext emptyList()
        try {
            val bpClass = Class.forName("androidx.health.connect.client.records.BloodPressureRecord")
            val timeRangeFilterClass = Class.forName("androidx.health.connect.client.time.TimeRangeFilter")
            val between = timeRangeFilterClass.getMethod("between", java.time.Instant::class.java, java.time.Instant::class.java)
            val timeRange = between.invoke(null, start.toInstant(), end.toInstant())
            val requestBuilderClass = Class.forName("androidx.health.connect.client.request.ReadRecordsRequestUsingFilters\$Builder")
            val builder = requestBuilderClass.getConstructor(Class::class.java).newInstance(bpClass)
            builder.javaClass.getMethod("setTimeRangeFilter", timeRangeFilterClass).invoke(builder, timeRange)
            val request = builder.javaClass.getMethod("build").invoke(builder) ?: return@withContext emptyList()
            val records = readRecordsList(c, request) ?: return@withContext emptyList()
            records.mapNotNull { record ->
                try {
                    val startTime = record?.javaClass?.getMethod("getTime")?.invoke(record) as? java.time.Instant
                        ?: record?.javaClass?.getMethod("getStartTime")?.invoke(record) as? java.time.Instant
                    val systolic = (record?.javaClass?.getMethod("getSystolic")?.invoke(record) as? Number)?.toDouble() ?: return@mapNotNull null
                    val diastolic = (record?.javaClass?.getMethod("getDiastolic")?.invoke(record) as? Number)?.toDouble() ?: return@mapNotNull null
                    if (startTime != null) BloodPressureReading(systolic, diastolic, Date.from(startTime)) else null
                } catch (_: Throwable) { null }
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }

    private suspend fun readSamples(
        recordClass: Class<*>,
        start: Date,
        end: Date,
        type: HealthDataType,
        valueGetter: (Any) -> Double,
        unit: String
    ): List<HealthDataPoint> = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext emptyList()
        try {
            val timeRangeFilterClass = Class.forName("androidx.health.connect.client.time.TimeRangeFilter")
            val between = timeRangeFilterClass.getMethod("between", java.time.Instant::class.java, java.time.Instant::class.java)
            val timeRange = between.invoke(null, start.toInstant(), end.toInstant())
            val requestBuilderClass = Class.forName("androidx.health.connect.client.request.ReadRecordsRequestUsingFilters\$Builder")
            val builder = requestBuilderClass.getConstructor(Class::class.java).newInstance(recordClass)
            builder.javaClass.getMethod("setTimeRangeFilter", timeRangeFilterClass).invoke(builder, timeRange)
            val request = builder.javaClass.getMethod("build").invoke(builder) ?: return@withContext emptyList()
            val records = readRecordsList(c, request) ?: return@withContext emptyList()
            records.mapNotNull { record ->
                try {
                    val startTime = record?.javaClass?.getMethod("getStartTime")?.invoke(record) as? java.time.Instant
                    val endTime = record?.javaClass?.getMethod("getEndTime")?.invoke(record) as? java.time.Instant
                    val value = valueGetter(record!!)
                    val source = record.javaClass.getMethod("getMetadata").invoke(record)?.let { m -> m?.javaClass?.getMethod("getDataOrigin")?.invoke(m)?.toString() } ?: "Health Connect"
                    if (startTime != null && endTime != null)
                        HealthDataPoint(type, value, unit, Date.from(startTime), Date.from(endTime), source, source)
                    else null
                } catch (_: Throwable) { null }
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }

    override suspend fun getLastSyncedAt(): Long? = cache.getLastSyncedAt()

    override suspend fun setLastSyncedAt() { cache.setLastSyncedAt() }

    private fun Date.toInstant(): java.time.Instant = java.time.Instant.ofEpochMilli(time)

    companion object {
        private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

        private val HEALTH_TYPE_TO_RECORD_CLASS: Map<HealthDataType, Class<*>> by lazy {
            try {
                mapOf(
                    HealthDataType.STEPS to Class.forName("androidx.health.connect.client.records.StepsRecord"),
                    HealthDataType.HEART_RATE to Class.forName("androidx.health.connect.client.records.HeartRateRecord"),
                    HealthDataType.CALORIES to Class.forName("androidx.health.connect.client.records.ActiveCaloriesBurnedRecord"),
                    HealthDataType.SLEEP to Class.forName("androidx.health.connect.client.records.SleepSessionRecord"),
                    HealthDataType.DISTANCE to Class.forName("androidx.health.connect.client.records.DistanceRecord"),
                    HealthDataType.SPO2 to Class.forName("androidx.health.connect.client.records.OxygenSaturationRecord"),
                    HealthDataType.BODY_MASS to Class.forName("androidx.health.connect.client.records.WeightRecord"),
                    HealthDataType.BLOOD_PRESSURE to Class.forName("androidx.health.connect.client.records.BloodPressureRecord")
                )
            } catch (_: Throwable) {
                emptyMap()
            }
        }
    }
}
