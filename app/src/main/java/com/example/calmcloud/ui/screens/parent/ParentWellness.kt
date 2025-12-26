package com.example.calmcloud.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calmcloud.data.repository.StudentRepository
import com.example.calmcloud.data.remote.dto.Child
import com.example.calmcloud.data.remote.dto.StudentFHIRData
import com.example.calmcloud.ui.components.LineChart
import com.example.calmcloud.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentWellness(userName: String) {
    var children by remember { mutableStateOf<List<Child>>(emptyList()) }
    var selectedChild by remember { mutableStateOf<Child?>(null) }
    var childData by remember { mutableStateOf<StudentFHIRData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var timeRange by remember { mutableIntStateOf(7) }
    val studentRepo = remember { StudentRepository() }

    LaunchedEffect(Unit) {
        studentRepo.getChildren()
            .onSuccess { childrenList ->
                children = childrenList
                selectedChild = childrenList.firstOrNull()
                selectedChild?.let { child ->
                    studentRepo.getStudentData(child.id)
                        .onSuccess { response ->
                            childData = response.fhirData
                            isLoading = false
                        }
                        .onFailure {
                            isLoading = false
                        }
                } ?: run { isLoading = false }
            }
            .onFailure {
                isLoading = false
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Wellness",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Profile */ }) {
                        Icon(Icons.Default.Person, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (selectedChild != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Child Selection Header
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Viewing Wellness for",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = selectedChild!!.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(CalmGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("😊", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }

                    // Time Range Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        FilterChip(
                            selected = timeRange == 7,
                            onClick = { timeRange = 7 },
                            label = { Text("7d") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = timeRange == 14,
                            onClick = { timeRange = 14 },
                            label = { Text("14d") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = timeRange == 30,
                            onClick = { timeRange = 30 },
                            label = { Text("30d") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    // Wellness Overview Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WellnessMetricCard(
                            title = "Overall Wellness",
                            value = "${calculateWellnessScore(selectedChild!!)}%",
                            icon = Icons.Default.Favorite,
                            color = CalmGreen,
                            modifier = Modifier.weight(1f)
                        )
                        WellnessMetricCard(
                            title = "Check-ins",
                            value = "${selectedChild!!.checkinCount}",
                            icon = Icons.Default.CheckCircle,
                            color = CalmBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Mood Trends Chart
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Mood Trends",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val moodData = getMoodChartData(childData, timeRange)
                            if (moodData.isNotEmpty()) {
                                LineChart(
                                    data = moodData,
                                    labels = getDateLabels(timeRange),
                                    color = CalmBlue
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No mood data available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Stress Trends Chart
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Stress Trends",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val stressData = getStressChartData(childData, timeRange)
                            if (stressData.isNotEmpty()) {
                                LineChart(
                                    data = stressData,
                                    labels = getDateLabels(timeRange),
                                    color = StatusUrgent
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No stress data available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Wellness Insights - Light grey with white inner area
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E5E5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        // White inner area with padding
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        tint = CalmBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Wellness Insights",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val (firstLine, secondLine) = getWellnessInsight(selectedChild!!, childData)
                                        Text(
                                            text = firstLine,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = secondLine,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Activity Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Activity Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            ActivityRow("Recent Mood", selectedChild!!.recentEmotion?.replaceFirstChar { it.uppercaseChar() } ?: "No data", Icons.Default.Favorite)
                            ActivityRow("Stress Level", selectedChild!!.recentStress?.let { "$it/10" } ?: "No data", Icons.Default.Warning)
                            ActivityRow("Last Check-in", selectedChild!!.lastCheckinDate ?: "No data", Icons.Default.DateRange)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No child data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun WellnessMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ActivityRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = CalmBlue, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

fun calculateWellnessScore(child: Child): Int {
    val stressScore = child.recentStress?.let { 100 - (it * 10) } ?: 70
    val engagementScore = when {
        child.checkinCount >= 20 -> 100
        child.checkinCount >= 10 -> 80
        child.checkinCount >= 5 -> 60
        else -> 40
    }
    return ((stressScore * 0.7f + engagementScore * 0.3f).toInt()).coerceIn(0, 100)
}

fun getMoodChartData(data: StudentFHIRData?, days: Int): List<Float> {
    if (data == null) return emptyList()
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val dates = (0 until days).map { daysAgo ->
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        dateFormat.format(calendar.time)
    }.reversed()
    
    return dates.map { dateStr ->
        data.observations
            .filter { obs ->
                try {
                    val obsDateStr = obs.effectiveDateTime.substringBefore("T")
                    obsDateStr == dateStr && obs.code.coding.any { it.code == "75258-2" }
                } catch (_: Exception) {
                    false
                }
            }
            .mapNotNull {
                when (it.valueString) {
                    "happy" -> 9f
                    "calm" -> 7f
                    "okay" -> 5f
                    "sad" -> 3f
                    "anxious" -> 2f
                    "stressed" -> 1f
                    else -> 5f
                }
            }
            .average()
            .toFloat()
            .takeIf { !it.isNaN() } ?: 5f
    }
}

fun getStressChartData(data: StudentFHIRData?, days: Int): List<Float> {
    if (data == null) return emptyList()
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val dates = (0 until days).map { daysAgo ->
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        dateFormat.format(calendar.time)
    }.reversed()
    
    return dates.map { dateStr ->
        data.observations
            .filter { obs ->
                try {
                    val obsDateStr = obs.effectiveDateTime.substringBefore("T")
                    obsDateStr == dateStr && obs.code.coding.any { it.code == "73985-4" }
                } catch (_: Exception) {
                    false
                }
            }
            .mapNotNull { it.valueQuantity?.value?.toFloat() }
            .average()
            .toFloat()
            .takeIf { !it.isNaN() } ?: 5f
    }
}

fun getDateLabels(days: Int): List<String> {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    return (0 until days).map { daysAgo ->
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        dateFormat.format(calendar.time)
    }.reversed()
}

fun getWellnessInsight(child: Child, data: StudentFHIRData?): Pair<String, String> {
    val name = child.name
    val stress = child.recentStress ?: 5
    val checkIns = child.checkinCount
    
    return when {
        stress <= 3 && checkIns >= 10 -> 
            "$name is showing excellent wellness patterns with low stress and consistent check-ins." to 
            "Keep encouraging healthy habits!"
        stress <= 5 && checkIns >= 5 -> 
            "$name's wellness is stable." to 
            "Continue supporting their emotional well-being journey."
        stress >= 7 -> 
            "$name may benefit from additional support." to 
            "Consider reaching out to the care team for guidance."
        else -> 
            "$name is building healthy wellness habits." to 
            "Regular check-ins help track progress."
    }
}

