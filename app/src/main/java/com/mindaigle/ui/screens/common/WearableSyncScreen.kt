@file:Suppress("SpellCheckingInspection")
package com.mindaigle.ui.screens.common

import android.content.Intent
import androidx.core.content.edit
import androidx.core.net.toUri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.widget.Toast
import kotlinx.coroutines.delay
import com.mindaigle.HealthConnectPermissionActivity
import com.mindaigle.data.health.BloodPressureReading
import com.mindaigle.data.health.HealthConnectManager
import com.mindaigle.data.health.HealthConnectService
import com.mindaigle.data.health.HealthDataType
import com.mindaigle.data.health.HealthService
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

private const val TAG = "WearableSync"
private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
private const val HEALTH_CONNECT_PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE"
// Android 14+ (API 34): opens system UI to grant Health Connect permissions for our app
private const val ACTION_MANAGE_HEALTH_PERMISSIONS = "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS"
private const val PREF_HEALTH_ONBOARDING_SEEN = "health_onboarding_seen"

/** Starts an activity. Uses applicationContext and NEW_TASK so it works from any context. Shows Toast on failure. */
private fun startActivitySafe(context: android.content.Context, intent: Intent) {
    val appContext = context.applicationContext
    try {
        val i = Intent(intent).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        appContext.startActivity(i)
    } catch (e: Throwable) {
        Log.e(TAG, "startActivity failed: ${e.message}", e)
        Toast.makeText(appContext, "Could not open app.", Toast.LENGTH_SHORT).show()
    }
}

/** Opens Health Connect or the system health-permission screen so the user can grant access. Tries options in order; shows Toast if nothing works. */
private fun openHealthConnectPermissionScreen(context: android.content.Context) {
    val appContext = context.applicationContext
    fun start(intent: Intent): Boolean {
        val i = Intent(intent).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return try {
            appContext.startActivity(i)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "startActivity failed: ${e.message}", e)
            false
        }
    }
    // 1) Android 14+: system screen to manage health permissions for our app
    if (Build.VERSION.SDK_INT >= 34) {
        val intent = Intent(ACTION_MANAGE_HEALTH_PERMISSIONS).apply {
            putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
        }
        if (start(intent)) return
    }
    // 2) Health Connect app or its manage-data screen
    if (start(HealthConnectManager.getHealthConnectIntent(context))) return
    // 3) Play Store to install Health Connect
    val playIntent = Intent(Intent.ACTION_VIEW, HEALTH_CONNECT_PLAY_STORE_URL.toUri()).setPackage("com.android.vending")
    if (start(playIntent)) return
    val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$HEALTH_CONNECT_PACKAGE".toUri())
    if (start(marketIntent)) return
    Toast.makeText(appContext, "Could not open Health Connect. Install it from the Play Store.", Toast.LENGTH_LONG).show()
}
private const val BACKFILL_DAYS = 30
private const val FOREGROUND_HOURS = 24
private const val PREF_STEP_GOAL = "wearable_step_goal"
private const val PREF_SLEEP_GOAL_MIN = "wearable_sleep_goal_min"
private const val PREF_SYNC_TO_PROFILE = "wearable_sync_to_profile"
private const val DEFAULT_STEP_GOAL = 10_000L
private const val DEFAULT_SLEEP_GOAL_MIN = 420 // 7h

private data class SyncResult(
    val steps: Long?,
    val sleep: Long?,
    val calories: Double?,
    val heartRateLatest: Double?,
    val distanceKm: Double?,
    val spo2Latest: Double?,
    val bloodPressureLatest: BloodPressureReading?,
    val bodyMassLatest: Double?,
    val stepsHistory: List<Long>,
    val sleepHistory: List<Long>,
    val syncedAt: Long?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER") // userName kept for API consistency
fun WearableSyncScreen(
    userName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val healthService = remember { HealthConnectService(context) }
    val prefs = remember { context.getSharedPreferences("health_ui", android.content.Context.MODE_PRIVATE) }
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean(PREF_HEALTH_ONBOARDING_SEEN, false)) }
    var status by remember { mutableStateOf<HealthStatus>(HealthStatus.Loading) }
    var grantedTypes by remember { mutableStateOf<Set<HealthDataType>>(emptySet()) }
    var stepsToday by remember { mutableStateOf<Long?>(null) }
    var sleepMinutes by remember { mutableStateOf<Long?>(null) }
    var caloriesToday by remember { mutableStateOf<Double?>(null) }
    var heartRateLatest by remember { mutableStateOf<Double?>(null) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var spo2Latest by remember { mutableStateOf<Double?>(null) }
    var bloodPressureLatest by remember { mutableStateOf<BloodPressureReading?>(null) }
    var bodyMassLatest by remember { mutableStateOf<Double?>(null) }
    var stepsHistory by remember { mutableStateOf<List<Long>>(emptyList()) }
    var sleepHistory by remember { mutableStateOf<List<Long>>(emptyList()) }
    var lastSynced by remember { mutableStateOf<Long?>(null) }
    var showPermissionHint by remember { mutableStateOf(false) }
    var stepsAccessStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val studentRepo = remember { com.mindaigle.data.repository.StudentRepository() }

    fun loadStatusAndData() {
        status = HealthStatus.Loading
        stepsAccessStatus = null
        scope.launch {
            status = withContext(Dispatchers.IO) {
                when {
                    !healthService.isAvailable() -> HealthStatus.Unavailable
                    healthService.isUpdateRequired() -> HealthStatus.UpdateRequired
                    else -> HealthStatus.Available
                }
            }
            if (status == HealthStatus.Available) {
                var detected = healthService.getGrantedPermissions()
                stepsAccessStatus = if (detected.isEmpty()) "Steps access: Not granted" else "Steps access: Granted"
                val hadPermissionBeforeSync = detected.isNotEmpty()
                lastSynced = healthService.getLastSyncedAt()
                if (detected.isEmpty()) {
                    val calendar = Calendar.getInstance()
                    val end = calendar.time
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfToday = calendar.time
                    calendar.add(Calendar.HOUR_OF_DAY, -FOREGROUND_HOURS)
                    val start24h = calendar.time
                    val probed = mutableSetOf<HealthDataType>()
                    try { if (healthService.readStepsTotal(startOfToday, end) != null) probed.add(HealthDataType.STEPS) } catch (_: Throwable) { }
                    try { if (healthService.readSleepTotalMinutes(start24h, end) != null) probed.add(HealthDataType.SLEEP) } catch (_: Throwable) { }
                    try { if (healthService.readCaloriesBurnedTotal(startOfToday, end) != null) probed.add(HealthDataType.CALORIES) } catch (_: Throwable) { }
                    try {
                        calendar.time = end
                        calendar.add(Calendar.DAY_OF_YEAR, -30)
                        if (healthService.readBodyMass(calendar.time, end).isNotEmpty()) probed.add(HealthDataType.BODY_MASS)
                    } catch (_: Throwable) { }
                    if (probed.isNotEmpty()) detected = probed
                    if (detected.isEmpty()) detected = setOf(HealthDataType.STEPS)
                }
                grantedTypes = detected
                syncAndRefresh(healthService, grantedTypes) { data ->
                    stepsToday = data.steps
                    sleepMinutes = data.sleep
                    caloriesToday = data.calories
                    heartRateLatest = data.heartRateLatest
                    distanceKm = data.distanceKm
                    spo2Latest = data.spo2Latest
                    bloodPressureLatest = data.bloodPressureLatest
                    bodyMassLatest = data.bodyMassLatest
                    stepsHistory = data.stepsHistory
                    sleepHistory = data.sleepHistory
                    if (data.syncedAt != null) lastSynced = data.syncedAt
                    showPermissionHint = data.steps == null && !hadPermissionBeforeSync
                    if (data.steps != null) Toast.makeText(context, "Steps: ${data.steps}", Toast.LENGTH_SHORT).show()
                    if (prefs.getBoolean(PREF_SYNC_TO_PROFILE, false) && (data.steps != null || data.sleep != null || data.heartRateLatest != null)) {
                        scope.launch {
                            studentRepo.syncActivityToBackend(
                                steps = data.steps,
                                sleepMinutes = data.sleep,
                                heartRate = data.heartRateLatest?.toInt()
                            )
                        }
                    }
                }
            }
        }
    }

    val permissionStrings = remember { HealthConnectManager.getRequiredPermissionStrings() }
    val permissionContract = remember { HealthConnectManager.createPermissionContract() }
    val permissionLauncher = rememberLauncherForActivityResult(permissionContract) { granted ->
        if (granted.isNotEmpty()) loadStatusAndData()
        else {
            // App was just "registered" with Health Connect; open it so user can find MindAigle under Steps → Access
            openHealthConnectPermissionScreen(context)
        }
    }
    // Launch dedicated Activity so the permission request runs from a real Activity — makes MindAigle appear in Health Connect
    val permissionActivityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) loadStatusAndData()
    }
    var autoPermissionRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { loadStatusAndData() }
    // Auto-refresh every 30 seconds so data stays current without a manual refresh button
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            loadStatusAndData()
        }
    }

    LaunchedEffect(status, autoPermissionRequested) {
        if (status == HealthStatus.Available && !autoPermissionRequested) {
            autoPermissionRequested = true
        }
    }

    if (showOnboarding) {
        HealthOnboardingScreen(
            onContinue = {
                prefs.edit { putBoolean(PREF_HEALTH_ONBOARDING_SEEN, true) }
                showOnboarding = false
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Health & Wearable",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Steps, sleep, and activity from your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadStatusAndData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(BackgroundLight)
                .padding(20.dp)
        ) {
            when (status) {
                HealthStatus.Loading -> {
                    LoadingState()
                }
                HealthStatus.Unavailable -> {
                    UnavailableCard(
                        title = "Health Connect not available",
                        message = "Install Health Connect from the Play Store to sync your wearable. Data from your watch or phone stays on your device.",
                        buttonText = "Open Play Store",
                        onClick = {
                            startActivitySafe(context, Intent(Intent.ACTION_VIEW, HEALTH_CONNECT_PLAY_STORE_URL.toUri()).setPackage("com.android.vending"))
                        }
                    )
                }
                HealthStatus.UpdateRequired -> {
                    UnavailableCard(
                        title = "Update Health Connect",
                        message = "Update the Health Connect app to continue syncing your health data.",
                        buttonText = "Update",
                        onClick = {
                            startActivitySafe(context, Intent(Intent.ACTION_VIEW, "market://details?id=$HEALTH_CONNECT_PACKAGE".toUri()).setPackage("com.android.vending"))
                        }
                    )
                }
                HealthStatus.Available -> {
                    // Status chip + last synced
                    HealthStatusChip(
                        hasData = stepsToday != null,
                        lastSynced = lastSynced,
                        accessStatus = stepsAccessStatus
                    )
                    Spacer(Modifier.height(20.dp))

                    if (grantedTypes.isEmpty()) {
                        PermissionRationaleCard(
                            context = context,
                            permissionStrings = permissionStrings,
                            onRequestPermission = {
                                showPermissionHint = false
                                permissionLauncher.launch(permissionStrings)
                            },
                            onTryPermissionScreen = {
                                showPermissionHint = false
                                try {
                                    permissionActivityLauncher.launch(Intent(context, HealthConnectPermissionActivity::class.java))
                                } catch (_: Throwable) {
                                    openHealthConnectPermissionScreen(context)
                                }
                            },
                            onPermissionGranted = { loadStatusAndData() },
                            onOpenHealthConnect = { openHealthConnectPermissionScreen(context) }
                        )
                    } else {
                        val stepGoal = prefs.getLong(PREF_STEP_GOAL, DEFAULT_STEP_GOAL)
                        val sleepGoalMin = prefs.getInt(PREF_SLEEP_GOAL_MIN, DEFAULT_SLEEP_GOAL_MIN)
                        var showStepGoalDialog by remember { mutableStateOf(false) }
                        var showSleepGoalDialog by remember { mutableStateOf(false) }

                        if (stepsToday == null && stepsHistory.isEmpty() && !showPermissionHint) {
                            EmptyStateCard(onConnect = { loadStatusAndData() })
                            Spacer(Modifier.height(20.dp))
                        }

                        SyncToProfileCard(
                            checked = prefs.getBoolean(PREF_SYNC_TO_PROFILE, false),
                            onCheckedChange = { enabled ->
                                prefs.edit { putBoolean(PREF_SYNC_TO_PROFILE, enabled) }
                                if (enabled && (stepsToday != null || sleepMinutes != null || heartRateLatest != null)) {
                                    scope.launch {
                                        studentRepo.syncActivityToBackend(stepsToday, sleepMinutes, heartRateLatest?.toInt())
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(16.dp))

                        StepsHeroCard(
                            stepsToday = stepsToday,
                            stepGoal = stepGoal,
                            stepsHistory = stepsHistory,
                            onNoDataMessage = "Grant access in Health Connect; data appears within 30 seconds.",
                            onEditGoal = { showStepGoalDialog = true }
                        )
                        if (showStepGoalDialog) StepGoalDialog(
                            currentGoal = stepGoal,
                            onDismiss = { showStepGoalDialog = false },
                            onConfirm = { newGoal ->
                                prefs.edit { putLong(PREF_STEP_GOAL, newGoal) }
                                showStepGoalDialog = false
                            }
                        )

                        val weeklySteps = (stepsHistory.getOrNull(0) ?: stepsToday ?: 0L) + stepsHistory.drop(1).sum()
                        val stepStreak = computeStepStreak(stepsHistory, stepsToday, stepGoal)
                        if (stepGoal > 0 && (stepsToday != null || stepsHistory.isNotEmpty())) {
                            Spacer(Modifier.height(12.dp))
                            GoalsAndStreakCard(
                                stepGoal = stepGoal,
                                stepsToday = stepsToday,
                                weeklySteps = weeklySteps,
                                stepStreak = stepStreak,
                                sleepGoalMin = sleepGoalMin,
                                sleepMinutes = sleepMinutes,
                                onEditStepGoal = { showStepGoalDialog = true },
                                onEditSleepGoal = { showSleepGoalDialog = true }
                            )
                            if (showSleepGoalDialog) SleepGoalDialog(
                                currentMinutes = sleepGoalMin,
                                onDismiss = { showSleepGoalDialog = false },
                                onConfirm = { newMin ->
                                    prefs.edit { putInt(PREF_SLEEP_GOAL_MIN, newMin) }
                                    showSleepGoalDialog = false
                                }
                            )
                        }
                        if (showPermissionHint) {
                            Spacer(Modifier.height(12.dp))
                            PermissionHintCard()
                        }
                        Spacer(Modifier.height(24.dp))

                        // Section label
                        Text(
                            "More metrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 2-column grid of metric cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (grantedTypes.contains(HealthDataType.SLEEP)) {
                                    val sleepGoalMin = prefs.getInt(PREF_SLEEP_GOAL_MIN, DEFAULT_SLEEP_GOAL_MIN)
                                    val sleep = sleepMinutes
                                    val metSleepGoal = sleep != null && sleep >= sleepGoalMin
                                    MetricTile(
                                        title = "Sleep",
                                        value = sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—",
                                        subtitle = when {
                                            sleepMinutes == null -> "Sync wearable"
                                            metSleepGoal -> "✓ Met ${sleepGoalMin / 60}h goal"
                                            else -> "Last night"
                                        },
                                        icon = Icons.Filled.Star,
                                        tint = CalmPurple,
                                        history = sleepHistory.take(7),
                                        showSparkline = true
                                    )
                                }
                                if (grantedTypes.contains(HealthDataType.CALORIES)) {
                                    MetricTile(
                                        title = "Active calories",
                                        value = caloriesToday?.let { "%.0f".format(it) } ?: "—",
                                        subtitle = if (caloriesToday != null) "kcal today" else "No data yet",
                                        icon = Icons.Filled.Star,
                                        tint = CalmOrange
                                    )
                                }
                                if (grantedTypes.contains(HealthDataType.HEART_RATE)) {
                                    MetricTile(
                                        title = "Heart rate",
                                        value = heartRateLatest?.let { "%.0f bpm".format(it) } ?: "—",
                                        subtitle = if (heartRateLatest != null) "Latest" else "No reading",
                                        icon = Icons.Filled.Favorite,
                                        tint = CalmPink
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (grantedTypes.contains(HealthDataType.DISTANCE)) {
                                    MetricTile(
                                        title = "Distance",
                                        value = distanceKm?.let { "%.2f km".format(it) } ?: "—",
                                        subtitle = if (distanceKm != null) "Today" else "No data yet",
                                        icon = Icons.Filled.Star,
                                        tint = CalmGreen
                                    )
                                }
                                if (grantedTypes.contains(HealthDataType.SPO2)) {
                                    MetricTile(
                                        title = "SpO₂",
                                        value = spo2Latest?.let { "%.0f%".format(it) } ?: "—",
                                        subtitle = if (spo2Latest != null) "Oxygen" else "No reading",
                                        icon = Icons.Filled.Star,
                                        tint = CalmBlueLight
                                    )
                                }
                                if (grantedTypes.contains(HealthDataType.BLOOD_PRESSURE)) {
                                    val bpText = bloodPressureLatest?.let { "${it.systolic.toInt()}/${it.diastolic.toInt()}" } ?: "—"
                                    MetricTile(
                                        title = "Blood pressure",
                                        value = bpText,
                                        subtitle = if (bloodPressureLatest != null) "mmHg" else "No reading",
                                        icon = Icons.Filled.Favorite,
                                        tint = CalmBlueDark
                                    )
                                }
                                if (grantedTypes.contains(HealthDataType.BODY_MASS) || bodyMassLatest != null) {
                                    MetricTile(
                                        title = "Weight",
                                        value = bodyMassLatest?.let { "%.1f kg".format(it) } ?: "—",
                                        subtitle = if (bodyMassLatest != null) "Latest" else "No data",
                                        icon = Icons.Filled.Star,
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }

                        if (stepsToday != null && (sleepMinutes == null || caloriesToday == null || heartRateLatest == null)) {
                            Spacer(Modifier.height(20.dp))
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CalmBlue.copy(alpha = 0.06f)),
                                border = BorderStroke(1.dp, CalmBlue.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Filled.Info, contentDescription = null, tint = CalmBlue, modifier = Modifier.size(24.dp))
                                    Text(
                                        "Enable more in Health Connect: Sleep, Heart rate, Active calories, Distance → Data and access → MindAigle ON. Data refreshes automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = CalmBlue, modifier = Modifier.size(48.dp))
            Text("Loading health data…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun HealthStatusChip(
    hasData: Boolean,
    lastSynced: Long?,
    accessStatus: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (hasData) CalmGreen.copy(alpha = 0.12f) else CalmOrange.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (hasData) CalmGreen.copy(alpha = 0.3f) else CalmOrange.copy(alpha = 0.3f))
        ) {
            Text(
                if (hasData) "Connected" else "No data yet",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (hasData) CalmGreen else CalmOrange
            )
        }
        if (lastSynced != null) {
            Text(
                "Synced ${java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(Date(lastSynced))}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        accessStatus?.let { status ->
            if (status.contains("Granted")) {
                Text("• Access granted", style = MaterialTheme.typography.bodySmall, color = CalmGreen)
            }
        }
    }
}

@Composable
private fun StepsHeroCard(
    stepsToday: Long?,
    stepGoal: Long,
    stepsHistory: List<Long>,
    onNoDataMessage: String,
    onEditGoal: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CalmBlue.copy(alpha = 0.08f),
                            CalmBlue.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CalmBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = CalmBlue
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Steps today",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        Text(
                            stepsToday?.toString() ?: "—",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (stepsToday == null) {
                            Text(
                                onNoDataMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            val goal = if (stepGoal > 0) stepGoal else DEFAULT_STEP_GOAL
                            val progress = (stepsToday.toFloat() / goal).coerceAtMost(1f)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "${(progress * 100).toInt()}% of ${goal / 1000}k goal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    "· Tap to edit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CalmBlue.copy(alpha = 0.8f),
                                    modifier = Modifier.clickable(onClick = onEditGoal)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CalmBlue.copy(alpha = 0.2f))
                                    .clickable(onClick = onEditGoal)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(CalmBlue)
                                )
                            }
                        }
                    }
                }
                if (stepsHistory.size >= 7) {
                    MiniSparkline(
                        values = stepsHistory.reversed(),
                        maxValue = (stepsHistory.maxOrNull() ?: stepGoal).toFloat().coerceAtLeast(1f),
                        tint = CalmBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniSparkline(
    values: List<Long>,
    maxValue: Float,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEach { value ->
            val heightRatio = (value.toFloat() / maxValue).coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(tint.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightRatio)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(4.dp))
                        .background(tint)
                )
            }
        }
    }
}

@Composable
private fun PermissionHintCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CalmOrange.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, CalmOrange.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = CalmOrange, modifier = Modifier.size(24.dp))
            Text(
                "In Health Connect: Steps → Access → turn ON MindAigle. Data refreshes automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    history: List<Long> = emptyList(),
    showSparkline: Boolean = false
) {
    var showDetail by remember { mutableStateOf(false) }
    if (showDetail && history.size >= 7) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            title = { Text(title, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("Last 7 days", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    MiniSparkline(values = history.reversed(), maxValue = history.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f, tint = tint)
                }
            },
            confirmButton = { TextButton(onClick = { showDetail = false }) { Text("Close", color = CalmBlue) } }
        )
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showSparkline && history.size >= 7) Modifier.clickable { showDetail = true } else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = tint)
                }
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (showSparkline && history.size >= 7) {
                Spacer(Modifier.height(8.dp))
                MiniSparkline(
                    values = history.reversed(),
                    maxValue = history.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f,
                    tint = tint
                )
            }
        }
    }
}

private fun computeStepStreak(stepsHistory: List<Long>, stepsToday: Long?, stepGoal: Long): Int {
    if (stepGoal <= 0) return 0
    var streak = 0
    val todaySteps = stepsToday ?: stepsHistory.getOrNull(0) ?: 0L
    if (todaySteps >= stepGoal) {
        streak++
        for (i in 1 until stepsHistory.size) {
            if ((stepsHistory.getOrNull(i) ?: 0L) >= stepGoal) streak++ else break
        }
    }
    return streak
}

@Composable
private fun SyncToProfileCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sync to my profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    "Steps, sleep & heart rate appear on Home and Trends",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CalmBlue)
            )
        }
    }
}

@Composable
private fun EmptyStateCard(onConnect: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⌚", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(16.dp))
            Text(
                "Connect your watch",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sync your wearable with Health Connect, then grant MindAigle access to see steps, sleep, and more here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Check connection")
            }
        }
    }
}

@Composable
private fun StepGoalDialog(
    currentGoal: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val options = listOf(6_000L, 8_000L, 10_000L, 12_000L, 15_000L)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily step goal", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { goal ->
                    FilterChip(
                        selected = currentGoal == goal,
                        onClick = { onConfirm(goal) },
                        label = { Text("${goal / 1000}k steps") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalmBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = CalmBlue) } }
    )
}

@Composable
private fun SleepGoalDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val options = listOf(360 to "6h", 420 to "7h", 480 to "8h", 540 to "9h")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep goal", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (min, label) ->
                    FilterChip(
                        selected = currentMinutes == min,
                        onClick = { onConfirm(min) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalmPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = CalmBlue) } }
    )
}

@Composable
private fun GoalsAndStreakCard(
    stepGoal: Long,
    stepsToday: Long?,
    weeklySteps: Long,
    stepStreak: Int,
    sleepGoalMin: Int,
    sleepMinutes: Long?,
    onEditStepGoal: () -> Unit,
    onEditSleepGoal: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CalmGreen.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, CalmGreen.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Goals & streaks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onEditStepGoal),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Steps", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        "${stepsToday ?: 0} / ${stepGoal / 1000}k",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (stepStreak > 0) {
                        Text("🔥 $stepStreak day streak", style = MaterialTheme.typography.bodySmall, color = CalmGreen)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onEditSleepGoal),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Sleep", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    val met = sleepMinutes != null && sleepMinutes >= sleepGoalMin
                    Text(
                        if (sleepMinutes != null) "${sleepMinutes / 60}h ${sleepMinutes % 60}m" else "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (met) CalmGreen else TextPrimary
                    )
                    Text(
                        if (met) "✓ Met goal" else "Goal: ${sleepGoalMin / 60}h",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Text(
                "Weekly steps: ${weeklySteps / 1000}k",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

private suspend fun syncAndRefresh(
    healthService: HealthService,
    grantedTypes: Set<HealthDataType>,
    onResult: (SyncResult) -> Unit
) {
    val calendar = Calendar.getInstance()
    val end = calendar.time
    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
    val startOfToday = calendar.time
    calendar.add(Calendar.HOUR_OF_DAY, -FOREGROUND_HOURS)
    val start24h = calendar.time

    var steps: Long? = null
    if (grantedTypes.contains(HealthDataType.STEPS)) {
        steps = healthService.readStepsTotal(startOfToday, end)
        if (steps == null) steps = healthService.readStepsTotal(start24h, end)
        if (steps == null) {
            calendar.time = end
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            steps = healthService.readStepsTotal(calendar.time, end)
        }
        if (steps == null) {
            calendar.time = end
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            steps = healthService.readStepsTotal(calendar.time, end)
        }
    }
    val sleep = if (grantedTypes.contains(HealthDataType.SLEEP)) healthService.readSleepTotalMinutes(start24h, end) else null
    val calories = if (grantedTypes.contains(HealthDataType.CALORIES)) healthService.readCaloriesBurnedTotal(startOfToday, end) else null

    val heartRateLatest = if (grantedTypes.contains(HealthDataType.HEART_RATE)) {
        healthService.readHeartRate(start24h, end).maxByOrNull { it.startDate.time }?.value
    } else null
    val distanceM = if (grantedTypes.contains(HealthDataType.DISTANCE)) {
        healthService.readDistance(startOfToday, end).sumOf { it.value }
    } else null
    val distanceKm = distanceM?.let { it / 1000.0 }
    val spo2Latest = if (grantedTypes.contains(HealthDataType.SPO2)) {
        healthService.readOxygenSaturation(start24h, end).maxByOrNull { it.startDate.time }?.value
    } else null
    val bloodPressureLatest = if (grantedTypes.contains(HealthDataType.BLOOD_PRESSURE)) {
        healthService.readBloodPressure(start24h, end).maxByOrNull { it.date.time }
    } else null

    var bodyMassLatest: Double? = null
    if (grantedTypes.contains(HealthDataType.BODY_MASS)) {
        try {
            calendar.time = end
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val massList = healthService.readBodyMass(calendar.time, end)
            bodyMassLatest = massList.maxByOrNull { it.startDate.time }?.value
        } catch (_: Throwable) { }
    }

    val stepsHistoryList = mutableListOf<Long>()
    val sleepHistoryList = mutableListOf<Long>()
    for (dayOffset in 0 until 7) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, -dayOffset)
        val dayStart = cal.time
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayEnd = cal.time
        if (grantedTypes.contains(HealthDataType.STEPS)) {
            stepsHistoryList.add(healthService.readStepsTotal(dayStart, dayEnd) ?: 0L)
        }
        cal.time = dayStart
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val nightStart = cal.time
        if (grantedTypes.contains(HealthDataType.SLEEP)) {
            sleepHistoryList.add(healthService.readSleepTotalMinutes(nightStart, dayStart) ?: 0L)
        }
    }
    if (stepsHistoryList.size < 7) while (stepsHistoryList.size < 7) stepsHistoryList.add(0L)
    if (sleepHistoryList.size < 7) while (sleepHistoryList.size < 7) sleepHistoryList.add(0L)

    val lastSynced = healthService.getLastSyncedAt()
    if (lastSynced == null) {
        calendar.time = end
        calendar.add(Calendar.DAY_OF_YEAR, -BACKFILL_DAYS)
        healthService.readStepsTotal(calendar.time, end)
        healthService.readSleepTotalMinutes(calendar.time, end)
        healthService.readCaloriesBurnedTotal(calendar.time, end)
    }
    healthService.setLastSyncedAt()
    val syncedAt = healthService.getLastSyncedAt()
    onResult(SyncResult(steps, sleep, calories, heartRateLatest, distanceKm, spo2Latest, bloodPressureLatest, bodyMassLatest, stepsHistoryList.take(7), sleepHistoryList.take(7), syncedAt))
}

@Composable
@Suppress("UNUSED_PARAMETER") // permissionStrings passed by caller for permissionLauncher.launch(permissionStrings)
private fun PermissionRationaleCard(
    context: android.content.Context,
    permissionStrings: Set<String>,
    onRequestPermission: () -> Unit,
    onTryPermissionScreen: (() -> Unit)?,
    onPermissionGranted: () -> Unit,
    onOpenHealthConnect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(CalmBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(36.dp), tint = CalmBlue)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Connect your wearable",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your steps, sleep, and activity stay on your device. We only read what you allow.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepRow(1, "Tap the button below to request access")
                StepRow(2, "In Health Connect, open Steps → Access → turn ON MindAigle")
                StepRow(3, "Return here — data refreshes automatically")
            }
            if (Build.VERSION.SDK_INT < 34) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "If MindAigle doesn’t appear: Settings → Apps → Health Connect → Force stop, then open Health Connect again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            GrantAccessButton(onRequestPermission = onRequestPermission)
            onTryPermissionScreen?.let { tryScreen ->
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = tryScreen, modifier = Modifier.fillMaxWidth()) {
                    Text("MindAigle not listed? Try permission screen", color = CalmBlue)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Checking…", Toast.LENGTH_SHORT).show()
                    onPermissionGranted()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I’ve granted access — check now")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenHealthConnect, modifier = Modifier.fillMaxWidth()) {
                Text("Open Health Connect app", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CalmBlue.copy(alpha = 0.15f)
        ) {
            Text(
                "$number",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = CalmBlue
            )
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GrantAccessButton(
    onRequestPermission: () -> Unit
) {
    Button(
        onClick = onRequestPermission,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        enabled = true,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
    ) {
        Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text("Grant access in Health Connect", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UnavailableCard(title: String, message: String, buttonText: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CalmBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(32.dp), tint = CalmBlue)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
            ) {
                Text(buttonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private sealed class HealthStatus {
    object Loading : HealthStatus()
    object Unavailable : HealthStatus()
    object UpdateRequired : HealthStatus()
    object Available : HealthStatus()
}
