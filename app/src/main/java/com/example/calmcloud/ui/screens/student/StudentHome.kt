package com.example.calmcloud.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calmcloud.data.remote.dto.FHIRObservation
import com.example.calmcloud.data.remote.dto.StudentFHIRData
import com.example.calmcloud.data.repository.StudentRepository
import com.example.calmcloud.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHome(userName: String) {
    var studentData by remember { mutableStateOf<StudentFHIRData?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val repository = remember { StudentRepository() }
    val scope = rememberCoroutineScope()

    // Load student data
    LaunchedEffect(Unit) {
        repository.getStudentData(1)
            .onSuccess { response ->
                studentData = response.fhirData
            }
    }

    // Get latest metrics
    val latestMood = getLatestMood(studentData)
    val latestStress = getLatestStress(studentData)
    val latestHeartRate = getLatestHeartRate(studentData)
    val latestWater = getLatestWater(studentData)
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
                        Text("👧", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = userName.split(" ").firstOrNull() ?: userName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Open drawer */ }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(Icons.Default.Notifications, null)
                    }
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.Person, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
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
                                    text = "${userName.split(" ").firstOrNull() ?: userName} 👧",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$greeting! How are you feeling today?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", style = MaterialTheme.typography.headlineSmall)
                            }
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
                        MetricCard(
                            title = "Water",
                            value = if (latestWater > 0) "$latestWater" else "--",
                            subtitle = "mL today",
                            icon = Icons.Default.Favorite,
                            iconColor = CalmBlue,
                            modifier = Modifier.weight(1f)
                        )
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
                            Icon(Icons.Default.Star, null, tint = CalmBlue, modifier = Modifier.size(24.dp))
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
                
                Spacer(modifier = Modifier.height(4.dp))

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
                        color = CalmBlue
                    )
                    QuickResourceCard(
                        emoji = "📚",
                        title = "Study Tips",
                        description = "Managing workload",
                        color = CalmBlueLight
                    )
                    QuickResourceCard(
                        emoji = "💬",
                        title = "Talk to Someone",
                        description = "Counselor available",
                        color = CalmBlueDark
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
fun QuickResourceCard(
    emoji: String,
    title: String,
    description: String,
    color: Color
) {
    Card(
        onClick = { /* Handle click */ },
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

fun getLatestMood(data: StudentFHIRData?): Triple<String, String, String> {
    if (data == null) return Triple("--", "No data", "😐")
    val moodObs = data.observations
        .filter { it.code.coding.any { c -> c.code == "75258-2" } }
        .maxByOrNull { it.effectiveDateTime }
    val mood = moodObs?.valueString ?: "okay"
    val emoji = when (mood) {
        "happy" -> "😊"
        "calm" -> "😌"
        "okay" -> "😐"
        "sad" -> "😢"
        "anxious" -> "😰"
        "stressed" -> "😫"
        else -> "😐"
    }
    return Triple(emoji, mood.replaceFirstChar { it.uppercaseChar() }, emoji)
}

fun getLatestStress(data: StudentFHIRData?): Int {
    if (data == null) return 5
    return data.observations
        .filter { it.code.coding.any { c -> c.code == "73985-4" } }
        .maxByOrNull { it.effectiveDateTime }
        ?.valueQuantity?.value?.toInt() ?: 5
}

fun getLatestHeartRate(data: StudentFHIRData?): Int {
    if (data == null) return 0
    return data.observations
        .filter { it.code.coding.any { c -> c.code == "8867-4" } }
        .maxByOrNull { it.effectiveDateTime }
        ?.valueQuantity?.value?.toInt() ?: 0
}

fun getLatestWater(data: StudentFHIRData?): Int {
    if (data == null) return 0
    return data.observations
        .filter { it.code.coding.any { c -> c.code == "9052-2" } }
        .maxByOrNull { it.effectiveDateTime }
        ?.valueQuantity?.value?.toInt() ?: 0
}

fun getWellnessTipForHome(stress: Int): String {
    return when {
        stress >= 8 -> "Take deep breaths. Try the 4-7-8 technique: inhale 4s, hold 7s, exhale 8s."
        stress >= 5 -> "Remember to take short breaks every hour. A 5-minute walk or stretch can boost your mood and focus!"
        else -> "Great job maintaining your wellness! Keep up the positive habits!"
    }
}

