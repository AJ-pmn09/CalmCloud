package com.mindaigle.ui.screens.parent

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
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.data.remote.dto.Child
import com.mindaigle.data.remote.dto.StudentFHIRData
import com.mindaigle.ui.components.LineChart
import com.mindaigle.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentWellness(
    userName: String,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var children by remember { mutableStateOf<List<Child>>(emptyList()) }
    var selectedChild by remember { mutableStateOf<Child?>(null) }
    var childData by remember { mutableStateOf<StudentFHIRData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var timeRange by remember { mutableIntStateOf(7) }
    val studentRepo = remember { StudentRepository() }

    LaunchedEffect(Unit) {
        // Initial load
        suspend fun loadData() {
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
        
        loadData()
        
        // Automatic periodic refresh every 30 seconds while screen is active
        while (true) {
            kotlinx.coroutines.delay(30000) // 30 seconds
            loadData()
        }
    }
    
    // Refresh when selected child changes
    LaunchedEffect(selectedChild?.id) {
        selectedChild?.let { child ->
            studentRepo.getStudentData(child.id)
                .onSuccess { response ->
                    childData = response.fhirData
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
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
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateToTab(0) }) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToTab(5) }) {
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

                    // Wellness Overview Cards (with word labels)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val wellnessScore = calculateWellnessScore(selectedChild!!)
                        WellnessMetricCard(
                            title = "Overall Wellness",
                            value = "$wellnessScore% (${wellnessScoreLabel(wellnessScore)})",
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
                                Text(
                                    text = "Mood scale: 9 = high, 1 = low. Labels: 7+ Good, 5–6 Okay, 3–4 Low, 1–2 Needs Attention.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
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
                                Text(
                                    text = "Scale: 0–2 Very low, 3–4 Low, 5–6 Moderate, 7–8 High, 9–10 Very high",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
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

                    // Dynamic summary cards: Avg stress (trend), Heart rate, Hydration, Correlation
                    val stressDataForCards = getStressChartData(childData, timeRange)
                    val avgStressForCards = stressDataForCards.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f
                    val trendIcon = getStressTrendIcon(stressDataForCards)
                    val avgHr = getAvgHeartRate(childData, timeRange)
                    val avgHydrationMl = getAvgHydrationMl(childData, timeRange)
                    val correlationPct = getStressHydrationCorrelationPercent(childData, timeRange)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StressSummaryCard(
                            title = "Avg stress",
                            value = String.format("%.1f", avgStressForCards),
                            subtitle = stressLevelLabel(avgStressForCards),
                            trend = trendIcon,
                            modifier = Modifier.weight(1f)
                        )
                        StressSummaryCard(
                            title = "Heart rate",
                            value = avgHr?.let { "${it.toInt()}" } ?: "--",
                            subtitle = "BPM",
                            modifier = Modifier.weight(1f)
                        )
                        StressSummaryCard(
                            title = "Hydration",
                            value = avgHydrationMl?.let { "${it.toInt()}" } ?: "--",
                            subtitle = "mL",
                            modifier = Modifier.weight(1f)
                        )
                        StressSummaryCard(
                            title = "Correlation",
                            value = correlationPct?.let { "$it%" } ?: "--",
                            subtitle = "stress vs low hydration",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Key Insights (stress trend, correlation, heart rate, positive feedback)
                    val keyInsights = getKeyInsights(selectedChild!!, childData, timeRange)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E5E5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Star, null, tint = CalmBlue, modifier = Modifier.size(24.dp))
                                        Text(
                                            text = "Key Insights",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                    keyInsights.forEach { line ->
                                        Text(
                                            text = "• $line",
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
                            ActivityRow("Stress Level", selectedChild!!.recentStress?.let { s -> "$s/10 (${stressLevelLabel(s.toFloat())})" } ?: "No data", Icons.Default.Warning)
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
fun StressSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    trend: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (trend != null) {
                    Text(text = trend, style = MaterialTheme.typography.titleSmall, color = CalmBlue)
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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

fun wellnessScoreLabel(score: Int): String = when {
    score >= 80 -> "Excellent"
    score in 60..79 -> "Good"
    score in 40..59 -> "Fair"
    score in 20..39 -> "Needs Attention"
    else -> "Critical"
}

fun stressLevelLabel(value: Float): String = when {
    value <= 2f -> "Very Low"
    value <= 4f -> "Low"
    value <= 6f -> "Moderate"
    value <= 8f -> "High"
    else -> "Very High"
}

fun getStressTrendIcon(stressData: List<Float>): String {
    if (stressData.size < 2) return "→"
    val mid = stressData.size / 2
    val firstAvg = stressData.take(mid).average()
    val secondAvg = stressData.drop(mid).average()
    return when {
        secondAvg < firstAvg - 0.5 -> "↓"
        secondAvg > firstAvg + 0.5 -> "↑"
        else -> "→"
    }
}

fun getAvgHeartRate(data: StudentFHIRData?, days: Int): Double? {
    if (data == null) return null
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dates = (0 until days).map { daysAgo ->
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        dateFormat.format(calendar.time)
    }.toSet()
    val values = data.observations
        .filter { obs ->
            try {
                obs.effectiveDateTime.substringBefore("T") in dates &&
                    obs.code.coding.any { it.code == "8867-4" }
            } catch (_: Exception) { false }
        }
        .mapNotNull { it.valueQuantity?.value }
    return values.takeIf { it.isNotEmpty() }?.average()
}

fun getAvgHydrationMl(data: StudentFHIRData?, days: Int): Double? {
    if (data == null) return null
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dates = (0 until days).map { daysAgo ->
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        dateFormat.format(calendar.time)
    }.toSet()
    val values = data.observations
        .filter { obs ->
            try {
                obs.effectiveDateTime.substringBefore("T") in dates &&
                    obs.code.coding.any { it.code == "9052-2" }
            } catch (_: Exception) { false }
        }
        .mapNotNull { it.valueQuantity?.value }
    return values.takeIf { it.isNotEmpty() }?.sum()
}

/** Returns correlation description: % of high-stress days that had low hydration (or null if not enough data). */
fun getStressHydrationCorrelationPercent(data: StudentFHIRData?, days: Int): Int? {
    if (data == null) return null
    val stressData = getStressChartData(data, days)
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dates = (0 until days).map { daysAgo ->
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        dateFormat.format(calendar.time)
    }
    var highStressDays = 0
    var highStressWithLowHydration = 0
    dates.forEachIndexed { i, dateStr ->
        val stress = stressData.getOrNull(i) ?: 5f
        if (stress >= 7f) {
            highStressDays++
            val hydrationMl = data.observations
                .filter { it.effectiveDateTime.startsWith(dateStr) && it.code.coding.any { c -> c.code == "9052-2" } }
                .mapNotNull { it.valueQuantity?.value }
                .sum()
            if (hydrationMl < 1000) highStressWithLowHydration++
        }
    }
    if (highStressDays == 0) return null
    return ((highStressWithLowHydration.toFloat() / highStressDays) * 100).toInt()
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

fun getKeyInsights(
    child: Child,
    data: StudentFHIRData?,
    timeRange: Int
): List<String> {
    val name = child.name ?: "Your child"
    val stressData = getStressChartData(data, timeRange)
    val trendIcon = getStressTrendIcon(stressData)
    val avgStress = stressData.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 5f
    val avgHr = getAvgHeartRate(data, timeRange)
    val avgHydration = getAvgHydrationMl(data, timeRange)
    val correlation = getStressHydrationCorrelationPercent(data, timeRange)
    val insights = mutableListOf<String>()
    insights.add(
        when (trendIcon) {
            "↓" -> "Stress trend: Improving over the period — stress levels have decreased."
            "↑" -> "Stress trend: Stress levels have increased recently. Consider checking in more often."
            else -> "Stress trend: Stable over the selected period."
        }
    )
    if (correlation != null && correlation > 0) {
        insights.add("On $correlation% of high-stress days, hydration was below 1L. Encouraging more water on busy days may help.")
    } else if (correlation != null) {
        insights.add("Good pattern: high-stress days don’t consistently coincide with low hydration.")
    }
    if (avgHr != null) {
        insights.add("Average heart rate: ${avgHr.toInt()} BPM.")
    }
    insights.add(
        when {
            avgStress <= 3f -> "Positive: $name is reporting low stress overall. Keep supporting their routines."
            avgStress <= 6f -> "Moderate stress levels. Regular check-ins and coping strategies are helpful."
            else -> "Consider extra support; stress levels have been elevated."
        }
    )
    return insights
}

