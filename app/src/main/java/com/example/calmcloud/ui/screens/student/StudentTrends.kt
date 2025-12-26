package com.example.calmcloud.ui.screens.student

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
import com.example.calmcloud.data.remote.dto.StudentFHIRData
import com.example.calmcloud.data.repository.StudentRepository
import com.example.calmcloud.ui.components.LineChart
import com.example.calmcloud.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTrends(userName: String) {
    var selectedMetric by remember { mutableStateOf("Mood") } // "Mood" or "Stress"
    var timeRange by remember { mutableIntStateOf(7) } // 7, 14, or 30 days
    var studentData by remember { mutableStateOf<StudentFHIRData?>(null) }
    val repository = remember { StudentRepository() }

    LaunchedEffect(Unit) {
        repository.getStudentData(1)
            .onSuccess { response ->
                studentData = response.fhirData
            }
    }

    val chartData = getChartData(studentData, selectedMetric, timeRange)
    val average = chartData.average().toFloat()
    val checkIns = chartData.size
    val trend = calculateTrend(chartData)

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
            TopAppBar(
                title = {
                    Text(
                        text = "Your Trends",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Back */ }) {
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

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mood/Stress Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedMetric == "Mood",
                            onClick = { selectedMetric = "Mood" },
                            label = { Text("Mood") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Favorite,
                                    null,
                                    tint = if (selectedMetric == "Mood") Color.White else TextSecondary
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmBlue,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedMetric == "Stress",
                            onClick = { selectedMetric = "Stress" },
                            label = { Text("Stress") },
                            leadingIcon = {
                                Text(
                                    text = "🧠",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmBlue,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
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
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = timeRange == 14,
                        onClick = { timeRange = 14 },
                        label = { Text("14d") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = timeRange == 30,
                        onClick = { timeRange = 30 },
                        label = { Text("30d") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        label = "Average",
                        value = String.format("%.1f", average),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Check-ins",
                        value = "$checkIns",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Trend",
                        value = trend,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Mood Over Time Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${selectedMetric} Over Time",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        LineChart(
                            data = chartData,
                            labels = getDateLabels(timeRange),
                            color = CalmBlue
                        )
                    }
                }

                // Insight Card - Light gray with white inner area
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
                                        text = "Insight",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = getInsight(selectedMetric, average),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
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
fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (label == "Trend") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = CalmBlue
                    )
            }
        }
    }
}

fun getChartData(data: StudentFHIRData?, metric: String, days: Int): List<Float> {
    if (data == null) return emptyList()
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val loincCode = if (metric == "Mood") "75258-2" else "73985-4"
    
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
                    obsDateStr == dateStr && obs.code.coding.any { it.code == loincCode }
                } catch (_: Exception) {
                    false
                }
            }
            .mapNotNull { 
                if (metric == "Mood") {
                    // Convert emotion to number (happy=9, calm=7, okay=5, sad=3, anxious=2, stressed=1)
                    when (it.valueString) {
                        "happy" -> 9f
                        "calm" -> 7f
                        "okay" -> 5f
                        "sad" -> 3f
                        "anxious" -> 2f
                        "stressed" -> 1f
                        else -> 5f
                    }
                } else {
                    it.valueQuantity?.value?.toFloat()
                }
            }
            .average()
            .toFloat()
            .takeIf { !it.isNaN() } ?: (if (metric == "Mood") 5f else 5f)
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

fun calculateTrend(data: List<Float>): String {
    if (data.size < 2) return "Stable"
    val firstHalf = data.take(data.size / 2).average()
    val secondHalf = data.drop(data.size / 2).average()
    return when {
        secondHalf > firstHalf + 0.5 -> "Improving"
        secondHalf < firstHalf - 0.5 -> "Declining"
        else -> "Stable"
    }
}

fun getInsight(metric: String, average: Float): String {
    return when (metric) {
        "Mood" -> when {
            average >= 7 -> "Your mood has been consistently positive! Keep up the great work with self-care."
            average >= 5 -> "Your mood is stable. Continue practicing self-care activities."
            else -> "Your mood could use some support. Consider trying breathing exercises or talking to someone."
        }
        "Stress" -> when {
            average <= 3 -> "Your stress levels are low - excellent! Keep up the healthy habits."
            average <= 6 -> "Your stress is moderate. Remember to take breaks and practice relaxation."
            else -> "Your stress levels are elevated. Consider trying stress-reduction techniques."
        }
        else -> "Keep tracking your wellness journey!"
    }
}

