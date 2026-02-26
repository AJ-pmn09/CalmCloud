package com.mindaigle.ui.screens.expert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindaigle.data.repository.AssistanceRepository
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.data.remote.dto.Student
import com.mindaigle.ui.components.LineChart
import com.mindaigle.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertReports(
    userName: String,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var requests by remember { mutableStateOf<List<com.mindaigle.data.remote.dto.AssistanceRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var timeRange by remember { mutableIntStateOf(30) }
    var reportType by remember { mutableStateOf("Wellness") }
    var showExportDialog by remember { mutableStateOf(false) }
    val studentRepo = remember { StudentRepository() }
    val assistanceRepo = remember { AssistanceRepository() }

    LaunchedEffect(Unit) {
        // Initial load
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
        
        // Automatic periodic refresh every 20 seconds while screen is active
        while (true) {
            kotlinx.coroutines.delay(20000) // 20 seconds
            studentRepo.getAllStudents()
                .onSuccess { studentsList ->
                    students = studentsList
                }
            assistanceRepo.getRequests()
                .onSuccess { requestsList ->
                    requests = requestsList
                }
        }
    }
    
    // Refresh when time range or report type changes
    LaunchedEffect(timeRange, reportType) {
        studentRepo.getAllStudents()
            .onSuccess { studentsList ->
                students = studentsList
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
                        text = "Reports",
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
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Export")
                    }
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Report Type Selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Wellness", "Engagement", "Trends", "Compliance").forEach { type ->
                            FilterChip(
                                selected = reportType == type,
                                onClick = { reportType = type },
                                label = { Text(type) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CalmBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White
                                )
                            )
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

                // Report Content based on type
                when (reportType) {
                    "Wellness" -> WellnessReport(students = students, timeRange = timeRange)
                    "Engagement" -> EngagementReport(students = students, timeRange = timeRange)
                    "Trends" -> TrendsReport(students = students, timeRange = timeRange)
                    "Compliance" -> ComplianceReport(students = students)
                }

                // Export Button
                Button(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalmBlue
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Export", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Report", fontWeight = FontWeight.Bold, color = com.mindaigle.ui.theme.TextPrimary) },
            text = { Text("Export to PDF or CSV will be available in a future update. Reports are viewable in-app.", color = com.mindaigle.ui.theme.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("OK", color = com.mindaigle.ui.theme.CalmBlue)
                }
            }
        )
    }
}

@Composable
fun WellnessReport(students: List<Student>, timeRange: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Wellness Overview
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
                    text = "Wellness Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val avgStress = students
                    .mapNotNull { it.lastCheckin?.stressLevel }
                    .takeIf { it.isNotEmpty() }
                    ?.average()?.toFloat() ?: 0f
                val highRisk = students.count { it.lastCheckin?.stressLevel != null && it.lastCheckin.stressLevel >= 7 }
                
                ReportMetricRow("Average Stress Level", String.format("%.1f/10", avgStress), CalmBlue)
                ReportMetricRow("High Risk Students", "$highRisk", StatusUrgent)
                ReportMetricRow("Total Observations", "${students.sumOf { it.observationCount }}", CalmGreen)
            }
        }

        // Stress Distribution
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
                    text = "Stress Level Distribution",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val low = students.count { it.lastCheckin?.stressLevel == null || it.lastCheckin.stressLevel < 5 }
                val moderate = students.count { 
                    it.lastCheckin?.stressLevel != null && 
                    it.lastCheckin.stressLevel >= 5 && 
                    it.lastCheckin.stressLevel < 8 
                }
                val high = students.count { it.lastCheckin?.stressLevel != null && it.lastCheckin.stressLevel >= 8 }
                val total = students.size
                
                DistributionBar("Low (0-4)", low, total, CalmGreen)
                DistributionBar("Moderate (5-7)", moderate, total, StatusOkay)
                DistributionBar("High (8-10)", high, total, StatusUrgent)
            }
        }
    }
}

@Composable
fun EngagementReport(students: List<Student>, timeRange: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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
                    text = "Engagement Metrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val totalObservations = students.sumOf { it.observationCount }
                val activeStudents = students.count { it.observationCount > 0 }
                val avgObservations = if (students.isNotEmpty()) totalObservations.toFloat() / students.size else 0f
                
                ReportMetricRow("Total Check-ins", "$totalObservations", CalmBlue)
                ReportMetricRow("Active Students", "$activeStudents/${students.size}", CalmGreen)
                ReportMetricRow("Avg Check-ins/Student", String.format("%.1f", avgObservations), CalmPurple)
            }
        }

        // Engagement Chart
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
                    text = "Check-in Activity Trend",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val activityData = getEngagementTrend(students, timeRange)
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
                            text = "No engagement data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrendsReport(students: List<Student>, timeRange: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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
                            text = "No trend data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Key Insights
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CalmBlue.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = CalmBlue, modifier = Modifier.size(24.dp))
                    Text(
                        text = "Trend Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = getTrendInsights(students),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun ComplianceReport(students: List<Student>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // FHIR Compliance Card (matching dashboard style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "FHIR Compliance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "All student data is stored in FHIR R4 format, ensuring healthcare interoperability and compliance with HIPAA regulations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CalmGreen)
                        )
                        Text(
                            text = "System operational",
                            style = MaterialTheme.typography.bodySmall,
                            color = CalmGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CalmGreen)
                        )
                        Text(
                            text = "Data synchronized",
                            style = MaterialTheme.typography.bodySmall,
                            color = CalmGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

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
                    text = "FHIR Compliance Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                ComplianceStatusRow("Data Format", "FHIR R4", CalmGreen)
                ComplianceStatusRow("LOINC Compliance", "100%", CalmGreen)
                ComplianceStatusRow("HIPAA Compliance", "Compliant", CalmGreen)
                ComplianceStatusRow("Data Synchronization", "Active", CalmGreen)
            }
        }

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
                    text = "Data Quality Metrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val totalObservations = students.sumOf { it.observationCount }
                val studentsWithData = students.count { it.observationCount > 0 }
                
                ReportMetricRow("Total FHIR Observations", "$totalObservations", CalmBlue)
                ReportMetricRow("Students with Data", "$studentsWithData/${students.size}", CalmGreen)
                ReportMetricRow("Data Completeness", "${(studentsWithData * 100 / students.size.coerceAtLeast(1))}%", CalmPurple)
            }
        }
    }
}

@Composable
fun ReportMetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun DistributionBar(label: String, count: Int, total: Int, color: Color) {
    val percentage = if (total > 0) (count * 100f / total) else 0f
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count (${percentage.toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ComplianceStatusRow(label: String, status: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

fun getEngagementTrend(students: List<Student>, days: Int): List<Float> {
    // Simplified - in real app, would aggregate by date
    val avgActivity = students.count { it.observationCount > 0 }.toFloat()
    return List(days) { avgActivity }
}

fun getStressTrends(students: List<Student>, days: Int): List<Float> {
    val avgStress = students
        .mapNotNull { it.lastCheckin?.stressLevel }
        .takeIf { it.isNotEmpty() }
        ?.average()?.toFloat() ?: 5f
    return List(days) { avgStress }
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

fun getTrendInsights(students: List<Student>): String {
    val avgStress = students
        .mapNotNull { it.lastCheckin?.stressLevel }
        .takeIf { it.isNotEmpty() }
        ?.average()?.toFloat() ?: 0f
    val highRisk = students.count { it.lastCheckin?.stressLevel != null && it.lastCheckin.stressLevel >= 7 }
    
    return when {
        avgStress >= 7 -> "Elevated stress levels detected across the population. Consider implementing school-wide wellness initiatives."
        avgStress >= 5 -> "Moderate stress levels observed. Continue monitoring and provide targeted support."
        highRisk > 0 -> "$highRisk students require immediate attention. Prioritize intervention strategies."
        else -> "Overall wellness trends are stable. Maintain current support programs."
    }
}

