package com.mindaigle.ui.screens.student

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.mindaigle.data.remote.dto.FHIRCode
import com.mindaigle.data.remote.dto.FHIRCoding
import com.mindaigle.data.remote.dto.FHIRObservation
import com.mindaigle.data.remote.dto.FHIRSubject
import com.mindaigle.data.remote.dto.FHIRValueQuantity
import com.mindaigle.data.remote.dto.StudentFHIRData
import com.mindaigle.data.remote.dto.ScreenerCatalogItem
import com.mindaigle.data.remote.dto.ScreenerInstance
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.data.repository.AppointmentRepository
import com.mindaigle.data.repository.ScreenerRepository
import com.mindaigle.ui.components.ProfilePicture
import com.mindaigle.ui.components.ProfilePictureShape
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val PREF_HYDRATION = "mindagile_hydration"
private fun hydrationKeyForDate(date: java.util.Date): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHome(
    userName: String,
    onNavigateToTab: (Int) -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenStaff: () -> Unit = {}
) {
    var studentData by remember { mutableStateOf<StudentFHIRData?>(null) }
    var latestCheckin by remember { mutableStateOf<com.mindaigle.data.remote.dto.LatestCheckin?>(null) }
    var activityData by remember { mutableStateOf<com.mindaigle.data.remote.dto.ActivityData?>(null) }
    var studentInfo by remember { mutableStateOf<com.mindaigle.data.remote.dto.StudentInfo?>(null) }
    var todayHydrationMl by remember { mutableIntStateOf(0) }
    var hydrationGoalMl by remember { mutableIntStateOf(2000) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLogWaterDialog by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val repository = remember { StudentRepository() }
    val screenerRepo = remember { ScreenerRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hydrationPrefs = remember { context.getSharedPreferences(PREF_HYDRATION, android.content.Context.MODE_PRIVATE) }
    var pendingScreeners by remember { mutableStateOf<List<ScreenerInstance>>(emptyList()) }
    var screenerCatalog by remember { mutableStateOf<List<ScreenerCatalogItem>>(emptyList()) }
    var selectedScreenerInstance by remember { mutableStateOf<ScreenerInstance?>(null) }
    var startScreenerLoading by remember { mutableStateOf(false) }
    var startScreenerError by remember { mutableStateOf<String?>(null) }

    // Automatic data loading and refresh
    LaunchedEffect(Unit) {
        try {
            repository.getMyStudentData()
                .onSuccess { response ->
                    studentData = response.fhirData
                    latestCheckin = response.latestCheckin
                    activityData = response.activityData
                    studentInfo = response.student
                    val fromApi = response.todayHydrationMl ?: 0
                    val fromObservations = computeTodayHydrationFromObservations(response.fhirData?.observations)
                    val apiOrObs = if (fromApi > 0) fromApi else fromObservations
                    val localMl = hydrationPrefs.getInt(hydrationKeyForDate(Date()), 0)
                    todayHydrationMl = maxOf(apiOrObs, localMl)
                    hydrationGoalMl = response.hydrationGoalMl ?: 2000
                    if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("StudentHome", "Data loaded: todayHydrationMl=$todayHydrationMl (api=$fromApi, fromObs=$fromObservations, local=$localMl)")
                }
                .onFailure { error ->
                    android.util.Log.e("StudentHome", "Failed to load student data: ${error.message}", error)
                }
            while (true) {
                kotlinx.coroutines.delay(30000)
                try {
                    repository.getMyStudentData()
                        .onSuccess { response ->
                            studentData = response.fhirData
                            latestCheckin = response.latestCheckin
                            activityData = response.activityData
                            studentInfo = response.student
                            val fromApi = response.todayHydrationMl ?: 0
                            val fromObservations = computeTodayHydrationFromObservations(response.fhirData?.observations)
                            val apiOrObs = if (fromApi > 0) fromApi else fromObservations
                            val localMl = hydrationPrefs.getInt(hydrationKeyForDate(Date()), 0)
                            todayHydrationMl = maxOf(apiOrObs, localMl)
                            hydrationGoalMl = response.hydrationGoalMl ?: 2000
                        }
                        .onFailure { error ->
                            android.util.Log.e("StudentHome", "Failed to refresh: ${error.message}", error)
                        }
                } catch (e: Throwable) {
                    android.util.Log.e("StudentHome", "Refresh error", e)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("StudentHome", "Data load error", e)
        }
    }

    var screenerLoadFailed by remember { mutableStateOf(false) }
    var screenerLoadAttempted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            screenerLoadFailed = false
            screenerRepo.getCatalog().onSuccess { screenerCatalog = it }.onFailure { screenerLoadFailed = true }
            screenerRepo.listMyInstances("assigned")
                .onSuccess { pendingScreeners = it }
                .onFailure { screenerLoadFailed = true; pendingScreeners = emptyList() }
        } catch (e: Throwable) {
            android.util.Log.e("StudentHome", "Screener load error", e)
            screenerLoadFailed = true
            pendingScreeners = emptyList()
        }
        screenerLoadAttempted = true
    }

    // Get latest metrics - use activityData and latestCheckin from actual database
    val latestMood = getLatestMood(activityData, latestCheckin)
    val latestStress = getLatestStress(activityData, latestCheckin)
    val latestHeartRate = getLatestHeartRate(activityData)
    val latestWater = getLatestWater(activityData)
    val greeting = getGreeting()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Header with Menu, Name, Bell, Profile
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfilePicture(
                            imageUrl = studentInfo?.profilePictureUrl,
                            userName = studentInfo?.name ?: studentInfo?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() } ?: userName,
                            size = 32.dp,
                            shape = ProfilePictureShape.Circle
                        )
                        Text(
                            text = userName.split(" ").firstOrNull() ?: userName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    Box {
                        IconButton(onClick = { showMenuDropdown = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false }
                        ) {
                                listOf(
                                    0 to "Home",
                                    1 to "Check-in",
                                    2 to "Trends",
                                    3 to "Appointments",
                                    4 to "Messages",
                                    5 to "Resources",
                                    6 to "Wearable",
                                    7 to "Settings"
                                ).forEach { (index, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            showMenuDropdown = false
                                            onNavigateToTab(index)
                                        }
                                    )
                                }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToTab(4) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Welcome Banner with Gradient
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(CalmBlue, CalmBlueLight, CalmBlueDark)
                        ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Welcome back,",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = userName.split(" ").firstOrNull() ?: userName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Time-based motivating message
                                var motivatingMessage by remember { mutableStateOf(getMotivatingMessage()) }
                                
                                // Update message every minute to check if time period changed
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        kotlinx.coroutines.delay(60000) // Check every minute
                                        motivatingMessage = getMotivatingMessage()
                                    }
                                }
                                
                                Text(
                                    text = motivatingMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            ProfilePicture(
                                imageUrl = studentInfo?.profilePictureUrl,
                                userName = studentInfo?.name ?: studentInfo?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() } ?: userName,
                                size = 48.dp,
                                shape = ProfilePictureShape.Rounded,
                                cornerRadius = 12.dp,
                                backgroundColor = Color.White.copy(alpha = 0.2f),
                                textColor = Color.White
                            )
                        }
                    }
                }

                // 2x2 Grid of Metric Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Mood",
                            value = latestMood.first,
                            subtitle = latestMood.second,
                            icon = Icons.Default.Favorite,
                            iconColor = StatusUrgent,
                            emoji = latestMood.third,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Stress",
                            value = "${latestStress}/10",
                            subtitle = when {
                                latestStress <= 3 -> "Low"
                                latestStress <= 6 -> "Moderate"
                                else -> "High"
                            },
                            icon = Icons.Default.Info,
                            iconColor = CalmPurple,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Heart Rate",
                            value = if (latestHeartRate > 0) "$latestHeartRate" else "--",
                            subtitle = "bpm",
                            icon = Icons.Default.Favorite,
                            iconColor = CalmPink,
                            modifier = Modifier.weight(1f)
                        )
                        HydrationFillCard(
                            currentMl = todayHydrationMl,
                            goalMl = hydrationGoalMl,
                            onLogWater = { showLogWaterDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Clinical Screeners (PHQ-9 / GAD-7 assigned by staff) — always show after load so screening "reflects"
                if (screenerLoadAttempted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Clinical screeners", tint = CalmBlue, modifier = Modifier.size(24.dp))
                                Text(
                                    text = "Clinical screeners (PHQ-9 & GAD-7)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            when {
                                screenerLoadFailed -> {
                                    Text(
                                        text = "Couldn't load screeners. Check your connection and retry.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Button(
                                        onClick = {
                                            screenerLoadFailed = false
                                            scope.launch {
                                                screenerRepo.getCatalog().onSuccess { screenerCatalog = it }.onFailure { screenerLoadFailed = true }
                                                screenerRepo.listMyInstances("assigned")
                                                    .onSuccess { pendingScreeners = it }
                                                    .onFailure { screenerLoadFailed = true }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                                    ) {
                                        Text("Retry")
                                    }
                                }
                                pendingScreeners.isEmpty() -> {
                                    Text(
                                        text = "Take a screening when you're ready. Your counselor can also assign PHQ-9 or GAD-7.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "You have screeners to complete. You can also start a new one below.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            // Take PHQ-9 / GAD-7 (student can self-start both)
                            if (studentInfo != null) {
                                startScreenerError?.let { msg ->
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            startScreenerError = null
                                            startScreenerLoading = true
                                            scope.launch {
                                                screenerRepo.createInstance(studentInfo!!.id, "phq9")
                                                    .onSuccess { newInstance ->
                                                        screenerRepo.listMyInstances("assigned").onSuccess { pendingScreeners = it }
                                                        selectedScreenerInstance = newInstance
                                                    }
                                                    .onFailure { e ->
                                                        startScreenerError = e.message ?: "Could not start PHQ-9. You may have completed it in the last 2 weeks."
                                                    }
                                                startScreenerLoading = false
                                            }
                                        },
                                        enabled = !startScreenerLoading,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = com.mindaigle.ui.theme.CalmBlue)
                                    ) {
                                        Text(if (startScreenerLoading) "…" else "Take PHQ-9")
                                    }
                                    Button(
                                        onClick = {
                                            startScreenerError = null
                                            startScreenerLoading = true
                                            scope.launch {
                                                screenerRepo.createInstance(studentInfo!!.id, "gad7")
                                                    .onSuccess { newInstance ->
                                                        screenerRepo.listMyInstances("assigned").onSuccess { pendingScreeners = it }
                                                        selectedScreenerInstance = newInstance
                                                    }
                                                    .onFailure { e ->
                                                        startScreenerError = e.message ?: "Could not start GAD-7. You may have completed it in the last 2 weeks."
                                                    }
                                                startScreenerLoading = false
                                            }
                                        },
                                        enabled = !startScreenerLoading,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = com.mindaigle.ui.theme.CalmPurple)
                                    ) {
                                        Text(if (startScreenerLoading) "…" else "Take GAD-7")
                                    }
                                }
                            }
                            pendingScreeners.forEach { instance ->
                                val typeKey = instance.screenerType.trim().lowercase()
                                val catalogItem = screenerCatalog.find { it.screenerType.trim().lowercase() == typeKey }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedScreenerInstance = instance },
                                    shape = RoundedCornerShape(12.dp),
                                    color = CalmBlue.copy(alpha = 0.08f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = catalogItem?.name ?: displayScreenerTypeName(instance.screenerType),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(text = "→", style = MaterialTheme.typography.titleMedium, color = CalmBlue)
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Wellness Tip
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CalmBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Screeners", tint = CalmBlue, modifier = Modifier.size(24.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Wellness Tip",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recommended for you",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = getWellnessTipForHome(latestStress),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
                
                // Recent Achievements (computed from activity logs)
                var achievementsData by remember { mutableStateOf<com.mindaigle.data.remote.dto.AchievementsData?>(null) }
                var achievementsLoading by remember { mutableStateOf(true) }
                val achievementRepo = remember { com.mindaigle.data.repository.AchievementRepository() }
                
                LaunchedEffect(Unit) {
                    achievementRepo.getComputedAchievements(30)
                        .onSuccess {
                            achievementsData = it
                            achievementsLoading = false
                            if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("StudentHome", "Achievements loaded: ${it.achievements.size} total, ${it.achievements.count { a -> a.unlocked }} unlocked")
                        }
                        .onFailure { error ->
                            achievementsLoading = false
                            android.util.Log.e("StudentHome", "Failed to load achievements", error)
                        }
                }
                
                // Achievements Section - iOS-style with vibrant colors
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🏆",
                                style = MaterialTheme.typography.displaySmall
                            )
                            Text(
                                text = "Your Achievements",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                        TextButton(onClick = onOpenAchievements) {
                            Text(
                                text = "See All →",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CalmBlue
                            )
                        }
                    }
                    
                    if (achievementsLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = CalmBlue,
                                strokeWidth = 3.dp
                            )
                        }
                    } else {
                        achievementsData?.let { data ->
                            val recentAchievements = data.achievements.filter { it.unlocked }.take(3)
                            if (recentAchievements.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    recentAchievements.forEach { achievement ->
                                        VibrantAchievementCard(
                                            achievement = achievement,
                                            modifier = Modifier.width(140.dp)
                                        )
                                    }
                                }
                            } else {
                                // Encouraging empty state
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFF4E6)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "🌟",
                                            style = MaterialTheme.typography.displayMedium
                                        )
                                        Text(
                                            text = "Start Your Journey!",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A1A1A)
                                        )
                                        Text(
                                            text = "Complete your daily check-ins to unlock amazing achievements!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } ?: run {
                            // Loading state
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                )
                            ) {
                                Text(
                                    text = "✨ Start tracking your activities to earn achievements!",
                                    modifier = Modifier.padding(20.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                // Quick Access to Support Team (reduced, with navigation)
                var availableStaff by remember { mutableStateOf<List<com.mindaigle.data.remote.dto.StaffMember>>(emptyList()) }
                val appointmentRepo = remember { com.mindaigle.data.repository.AppointmentRepository() }
                
                LaunchedEffect(Unit) {
                    appointmentRepo.getStaffAvailability()
                        .onSuccess {
                            availableStaff = it
                        }
                }
                
                if (availableStaff.isNotEmpty()) {
                    Card(
                        onClick = onOpenStaff,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFE8F5E9),
                                            Color(0xFFC8E6C9)
                                        )
                                    )
                                )
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "💚",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Your Support Team",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Text(
                                        text = "${availableStaff.size} staff members ready to help",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "➡️",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        }
                    }
                }
                
                // Quick Resources
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Resources",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("🎯", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        text = "Helpful tools for your wellness journey",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    QuickResourceCard(
                        emoji = "🧘",
                        title = "Breathing Exercise",
                        description = "5 minutes",
                        color = CalmBlue,
                        onClick = { onNavigateToTab(5) }
                    )
                    QuickResourceCard(
                        emoji = "📚",
                        title = "Study Tips",
                        description = "Managing workload",
                        color = CalmBlueLight,
                        onClick = { onNavigateToTab(5) }
                    )
                    QuickResourceCard(
                        emoji = "💬",
                        title = "Talk to Someone",
                        description = "Counselor available",
                        color = CalmBlueDark,
                        onClick = { onNavigateToTab(5) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showLogWaterDialog) {
            LogWaterDialog(
                onDismiss = { showLogWaterDialog = false },
                onLog = { ml ->
                    // Persist to local storage immediately so hydration survives navigation/refresh
                    val todayKey = hydrationKeyForDate(Date())
                    hydrationPrefs.edit {
                        putInt(todayKey, hydrationPrefs.getInt(todayKey, 0) + ml)
                    }
                    // Optimistic update so the water meter animates immediately
                    todayHydrationMl += ml
                    val goalReached = hydrationGoalMl > 0 && todayHydrationMl >= hydrationGoalMl
                    showLogWaterDialog = false
                    if (goalReached) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "🎉 Hydration goal achieved! Amazing job staying hydrated!",
                                withDismissAction = true
                            )
                        }
                    }
                    scope.launch {
                        val studentId = studentInfo?.id ?: 0
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        val timestamp = dateFormat.format(Date())
                        val obs = FHIRObservation(
                            id = "hydration-${System.currentTimeMillis()}",
                            code = FHIRCode(coding = listOf(FHIRCoding(system = "http://loinc.org", code = "9052-2", display = "Fluid intake"))),
                            valueQuantity = FHIRValueQuantity(value = ml.toDouble(), unit = "mL"),
                            effectiveDateTime = timestamp,
                            subject = FHIRSubject(reference = if (studentId != 0) "Patient/$studentId" else "Patient/1")
                        )
                        repository.saveStudentData(null, StudentFHIRData(observations = listOf(obs)), null)
                            .onSuccess {
                                kotlinx.coroutines.delay(500)
                                repository.getMyStudentData().onSuccess { r ->
                                    val fromApi = r.todayHydrationMl ?: 0
                                    val fromObs = computeTodayHydrationFromObservations(r.fhirData?.observations)
                                    val apiOrObs = when {
                                        fromApi > 0 -> fromApi
                                        fromObs > 0 -> fromObs
                                        else -> todayHydrationMl
                                    }
                                    val localMl = hydrationPrefs.getInt(hydrationKeyForDate(Date()), 0)
                                    todayHydrationMl = maxOf(apiOrObs, localMl)
                                    hydrationGoalMl = r.hydrationGoalMl ?: 2000
                                }
                            }
                            .onFailure {
                                snackbarHostState.showSnackbar("Couldn't sync water to server. Your log was added locally.")
                            }
                    }
                }
            )
        }

        selectedScreenerInstance?.let { instance ->
            val typeKey = instance.screenerType.trim().lowercase()
            val catalogItem = screenerCatalog.find { it.screenerType.trim().lowercase() == typeKey }
            ClinicalScreenerModal(
                instance = instance,
                catalogItem = catalogItem,
                studentId = instance.studentId,
                onDismiss = { selectedScreenerInstance = null },
                onCompleted = {
                    selectedScreenerInstance = null
                    scope.launch {
                        screenerRepo.listMyInstances("assigned").onSuccess { pendingScreeners = it }
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Sum today's fluid intake (mL) from FHIR observations (LOINC 9052-2) for initial/fallback display. */
private fun computeTodayHydrationFromObservations(observations: List<com.mindaigle.data.remote.dto.FHIRObservation>?): Int {
    if (observations.isNullOrEmpty()) return 0
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    return observations
        .filter { obs ->
            obs.code.coding.any { it.code == "9052-2" } &&
                obs.effectiveDateTime.startsWith(todayStr)
        }
        .sumOf { (it.valueQuantity?.value ?: 0.0).toInt() }
}

private fun displayScreenerTypeName(type: String): String {
    val t = type.trim().lowercase()
    return when (t) {
        "phq9" -> "PHQ-9"
        "gad7" -> "GAD-7"
        else -> type.uppercase()
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    emoji: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Icon(
                icon,
                null,
                tint = iconColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (emoji != null) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun HydrationFillCard(
    currentMl: Int,
    goalMl: Int,
    onLogWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fillRatio = if (goalMl > 0) (currentMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
    val animatedFill by animateFloatAsState(
        targetValue = fillRatio,
        animationSpec = tween(600),
        label = "hydration_fill"
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Water",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                // Sphere that fills from bottom (water level)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CalmBlue.copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedFill)
                            .align(Alignment.BottomCenter)
                            .background(CalmBlue)
                    )
                }
                Text(
                    text = "${currentMl} / $goalMl",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CalmBlue
                )
                Text(
                    text = "mL today",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                val goalAchieved = goalMl > 0 && currentMl >= goalMl
                if (goalAchieved) {
                    Text(
                        text = "🌟 Goal achieved! You're a hydration champion!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CalmGreen,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Button(
                    onClick = onLogWater,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CalmBlue),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (goalAchieved) "Add more (optional)" else "Log water",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

private val HYDRATION_MARKERS_ML = listOf(250, 500, 750, 1000, 1500, 2000)

@Composable
fun LogWaterDialog(
    onDismiss: () -> Unit,
    onLog: (Int) -> Unit
) {
    var sliderIndex by remember { mutableFloatStateOf(2f) } // default 750
    val selectedMl = HYDRATION_MARKERS_ML[sliderIndex.toInt().coerceIn(0, HYDRATION_MARKERS_ML.lastIndex)]
    val fillRatio = (selectedMl / 2000f).coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(targetValue = fillRatio, animationSpec = tween(400), label = "dialog_fill")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log water", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("How much water did you drink? (mL)", style = MaterialTheme.typography.bodyMedium)
                // Water fill preview (same animation as card)
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(CalmBlue.copy(alpha = 0.15f)))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedFill)
                            .align(Alignment.BottomCenter)
                            .background(CalmBlue)
                    )
                }
                Text(
                    text = "$selectedMl mL",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CalmBlue
                )
                Slider(
                    value = sliderIndex,
                    onValueChange = { sliderIndex = it.roundToInt().toFloat().coerceIn(0f, (HYDRATION_MARKERS_ML.size - 1).toFloat()) },
                    valueRange = 0f..(HYDRATION_MARKERS_ML.size - 1).toFloat(),
                    steps = HYDRATION_MARKERS_ML.size - 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HYDRATION_MARKERS_ML.forEach { ml ->
                        Text(
                            text = "$ml",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLog(selectedMl) },
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun QuickResourceCard(
    emoji: String,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Info, null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..21 -> "Good evening"
        else -> "Good night"
    }
}

fun getLatestMood(activityData: com.mindaigle.data.remote.dto.ActivityData?, checkin: com.mindaigle.data.remote.dto.LatestCheckin?): Triple<String, String, String> {
    // Use activityData.mood (string) first, then checkin.mood_rating (integer), then default
    val moodString = activityData?.mood?.lowercase() ?: when (checkin?.moodRating) {
        1 -> "sad"
        2 -> "anxious"
        3 -> "okay"
        4 -> "calm"
        5 -> "happy"
        else -> when (checkin?.mood) {
            1 -> "sad"
            2 -> "anxious"
            3 -> "okay"
            4 -> "calm"
            5 -> "happy"
            else -> "okay"
        }
    }
    
    val emoji = when (moodString) {
        "happy", "positive", "energetic" -> "😊"
        "calm", "relaxed" -> "😌"
        "okay", "neutral" -> "😐"
        "sad", "down" -> "😢"
        "anxious", "worried" -> "😰"
        "stressed", "frustrated" -> "😫"
        else -> "😐"
    }
    return Triple(emoji, moodString.replaceFirstChar { it.uppercaseChar() }, emoji)
}

fun getLatestStress(activityData: com.mindaigle.data.remote.dto.ActivityData?, checkin: com.mindaigle.data.remote.dto.LatestCheckin?): Int {
    // Use activityData.stressLevel first, then checkin.stressLevel
    return activityData?.stressLevel ?: checkin?.stressLevel ?: 5
}

fun getLatestHeartRate(activityData: com.mindaigle.data.remote.dto.ActivityData?): Int {
    return activityData?.heartRate ?: 0
}

fun getLatestWater(activityData: com.mindaigle.data.remote.dto.ActivityData?): Int {
    return activityData?.hydrationPercent ?: 0
}

fun getWellnessTipForHome(stress: Int): String {
    return when {
        stress >= 8 -> "Take deep breaths. Try the 4-7-8 technique: inhale 4s, hold 7s, exhale 8s."
        stress >= 5 -> "Remember to take short breaks every hour. A 5-minute walk or stretch can boost your mood and focus!"
        else -> "Great job maintaining your wellness! Keep up the positive habits!"
    }
}

@Composable
fun VibrantAchievementCard(
    achievement: com.mindaigle.data.remote.dto.ComputedAchievement,
    modifier: Modifier = Modifier
) {
    // Color gradients based on achievement type (iOS-style vibrant colors)
    val gradientColors = when {
        achievement.title.contains("First", ignoreCase = true) -> listOf(
            Color(0xFFFF6B6B), // Coral Red
            Color(0xFFFF8E8E)
        )
        achievement.title.contains("Week", ignoreCase = true) -> listOf(
            Color(0xFF4ECDC4), // Turquoise
            Color(0xFF6EDDD6)
        )
        achievement.title.contains("Month", ignoreCase = true) -> listOf(
            Color(0xFFFFD93D), // Golden Yellow
            Color(0xFFFFE66D)
        )
        achievement.title.contains("Step", ignoreCase = true) -> listOf(
            Color(0xFF95E1D3), // Mint Green
            Color(0xFFB4F0E2)
        )
        achievement.title.contains("Sleep", ignoreCase = true) -> listOf(
            Color(0xFFA8E6CF), // Soft Green
            Color(0xFFC8F5DD)
        )
        achievement.title.contains("Hydration", ignoreCase = true) -> listOf(
            Color(0xFF6C5CE7), // Purple
            Color(0xFF8B7EE8)
        )
        achievement.title.contains("Streak", ignoreCase = true) -> listOf(
            Color(0xFFFF7675), // Pink Red
            Color(0xFFFF9998)
        )
        else -> listOf(
            Color(0xFF74B9FF), // Sky Blue
            Color(0xFF95C9FF)
        )
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large emoji/icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Color.White.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = achievement.icon,
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                
                // Achievement title
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2
                )
                
                // Progress indicator for locked achievements
                if (!achievement.unlocked && achievement.progress > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { achievement.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "${achievement.progress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (achievement.unlocked) {
                    // Unlocked badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "✨ Unlocked!",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComputedAchievementBadge(
    achievement: com.mindaigle.data.remote.dto.ComputedAchievement,
    modifier: Modifier = Modifier
) {
    VibrantAchievementCard(achievement = achievement, modifier = modifier)
}

/**
 * Get time-based motivating message that changes throughout the day
 */
fun getMotivatingMessage(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    
    val messages = when {
        hour in 5..8 -> listOf(
            "Rise and shine! 🌅 Today is a fresh start.",
            "Good morning! You've got this! 💪",
            "Morning vibes! Let's make today amazing! ✨",
            "Start your day with a positive mindset! 🌟",
            "Every morning brings new possibilities! 🌄"
        )
        hour in 9..11 -> listOf(
            "You're doing great! Keep up the momentum! 🚀",
            "Mid-morning energy! Stay focused and positive! 💫",
            "You're making progress! Keep going! ⭐",
            "Your efforts matter! Keep pushing forward! 🌈",
            "Stay strong and keep moving forward! 💙"
        )
        hour in 12..14 -> listOf(
            "Lunch break! Take a moment to recharge! 🍽️",
            "Halfway through the day! You're doing well! 🌞",
            "Keep that positive energy flowing! ⚡",
            "You're stronger than you think! 💪",
            "Take a deep breath and keep going! 🌸"
        )
        hour in 15..17 -> listOf(
            "Afternoon power! You've got this! 🔥",
            "Keep pushing through! You're almost there! 🌟",
            "Your resilience is inspiring! 💎",
            "Stay focused! Great things are ahead! ✨",
            "You're handling today like a champion! 🏆"
        )
        hour in 18..20 -> listOf(
            "Evening time! Reflect on your achievements today! 🌆",
            "You made it through the day! Well done! 🎉",
            "Evening vibes! Time to unwind and relax! 🌙",
            "You accomplished a lot today! Be proud! 🌺",
            "Take time to appreciate how far you've come! 💜"
        )
        hour in 21..23 -> listOf(
            "Night time! Rest well, you've earned it! 🌃",
            "End your day with gratitude! You did great! 🌌",
            "Time to unwind and prepare for tomorrow! 🌠",
            "You navigated today beautifully! Rest easy! 💤",
            "Tomorrow is a new opportunity! Sleep well! 🌙"
        )
        else -> listOf(
            "Late night? Remember to take care of yourself! 🌜",
            "You're doing your best! That's what matters! 💙",
            "Rest when you need to. You're important! 🌸",
            "Take care of yourself, always! 💚",
            "You matter! Make sure to rest! 🌙"
        )
    }
    
    // Add weekend-specific messages
    val weekendMessages = if (isWeekend) listOf(
        "Weekend vibes! Enjoy your well-deserved break! 🎊",
        "Weekend time! Relax and recharge! 🌈",
        "You've earned this weekend! Enjoy it! 🎈"
    ) else emptyList()
    
    // Combine messages and pick one based on current time (for variety)
    val allMessages = messages + weekendMessages
    val index = (hour + dayOfWeek) % allMessages.size
    return allMessages[index]
}


