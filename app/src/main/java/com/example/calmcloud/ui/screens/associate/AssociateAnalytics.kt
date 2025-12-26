package com.example.calmcloud.ui.screens.associate

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calmcloud.data.repository.AssistanceRepository
import com.example.calmcloud.data.repository.StudentRepository
import com.example.calmcloud.data.remote.dto.Student
import com.example.calmcloud.ui.components.LineChart
import com.example.calmcloud.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociateAnalytics(userName: String) {
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var requests by remember { mutableStateOf<List<com.example.calmcloud.data.remote.dto.AssistanceRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var timeRange by remember { mutableIntStateOf(7) }
    val studentRepo = remember { StudentRepository() }
    val assistanceRepo = remember { AssistanceRepository() }

    LaunchedEffect(Unit) {
        studentRepo.getAllStudents()
            .onSuccess { studentsList ->
                students = studentsList
            }
        assistanceRepo.getRequests()
            .onSuccess { requestsList ->
                requests = requestsList
                isLoading = false
            }
            .onFailure {
                isLoading = false
            }
    }

    val totalStudents = students.size
    val avgStress = students
        .mapNotNull { it.lastCheckin?.stressLevel }
        .takeIf { it.isNotEmpty() }
        ?.average()?.toFloat() ?: 0f
    val activeCheckIns = students.count { it.lastCheckin != null }
    val highRiskCount = students.count { it.lastCheckin?.stressLevel != null && it.lastCheckin.stressLevel >= 7 }

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
                        text = "Analytics",
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Key Metrics Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsMetricCard(
                        title = "Total Students",
                        value = "$totalStudents",
                        icon = Icons.Default.Person,
                        color = CalmBlue,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMetricCard(
                        title = "Avg Stress",
                        value = String.format("%.1f", avgStress),
                        icon = Icons.Default.Warning,
                        color = StatusUrgent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsMetricCard(
                        title = "Active Check-ins",
                        value = "$activeCheckIns",
                        icon = Icons.Default.CheckCircle,
                        color = CalmGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMetricCard(
                        title = "High Risk",
                        value = "$highRiskCount",
                        icon = Icons.Default.Warning,
                        color = StatusUrgent,
                        modifier = Modifier.weight(1f)
                    )
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
                            text = "Average Stress Trends",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val stressData = getStressTrends(students, timeRange)
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

                // Check-in Activity Chart
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
                            text = "Check-in Activity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val activityData = getCheckInActivity(students, timeRange)
                        if (activityData.isNotEmpty()) {
                            LineChart(
                                data = activityData,
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
                                    text = "No activity data available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Student Status Distribution
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
                            text = "Student Status Distribution",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val goodCount = students.count { it.lastCheckin?.stressLevel == null || it.lastCheckin.stressLevel < 5 }
                        val monitorCount = students.count { 
                            it.lastCheckin?.stressLevel != null && 
                            it.lastCheckin.stressLevel >= 5 && 
                            it.lastCheckin.stressLevel < 8 
                        }
                        val criticalCount = students.count { it.lastCheckin?.stressLevel != null && it.lastCheckin.stressLevel >= 8 }
                        
                        StatusDistributionRow("Good", goodCount, totalStudents, CalmGreen)
                        StatusDistributionRow("Monitor", monitorCount, totalStudents, StatusOkay)
                        StatusDistributionRow("Critical", criticalCount, totalStudents, StatusUrgent)
                    }
                }

                // Insights Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CalmBlue.copy(alpha = 0.1f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Star, null, tint = CalmBlue, modifier = Modifier.size(24.dp))
                            Text(
                                text = "Analytics Insights",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = getAnalyticsInsight(students, requests, avgStress),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsMetricCard(
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
fun StatusDistributionRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val percentage = if (total > 0) (count * 100f / total) else 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "(${percentage.toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

fun getStressTrends(students: List<Student>, days: Int): List<Float> {
    // Simplified - in real app, would aggregate stress data by date
    val avgStress = students
        .mapNotNull { it.lastCheckin?.stressLevel }
        .takeIf { it.isNotEmpty() }
        ?.average()?.toFloat() ?: 5f
    return List(days) { avgStress }
}

fun getCheckInActivity(students: List<Student>, days: Int): List<Float> {
    // Simplified - in real app, would count check-ins per day
    val activeCount = students.count { it.lastCheckin != null }.toFloat()
    return List(days) { activeCount }
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

fun getAnalyticsInsight(
    students: List<Student>,
    requests: List<com.example.calmcloud.data.remote.dto.AssistanceRequest>,
    avgStress: Float
): String {
    val totalStudents = students.size
    val pendingRequests = requests.count { it.status == "pending" }
    
    return when {
        avgStress >= 7 && pendingRequests > 0 -> 
            "High average stress levels detected with pending requests. Consider prioritizing follow-ups with students showing elevated stress."
        avgStress >= 5 -> 
            "Moderate stress levels observed. Continue monitoring and provide support as needed."
        pendingRequests > 0 -> 
            "There are $pendingRequests pending assistance requests requiring attention."
        else -> 
            "Overall wellness metrics are stable. Students are actively engaging with check-ins."
    }
}

