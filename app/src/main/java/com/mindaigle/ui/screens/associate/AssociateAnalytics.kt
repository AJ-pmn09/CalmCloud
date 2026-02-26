package com.mindaigle.ui.screens.associate

import androidx.compose.foundation.background
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
import com.mindaigle.data.repository.AssistanceRepository
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.data.repository.AnalyticsRepository
import com.mindaigle.data.remote.dto.Student
import com.mindaigle.data.remote.dto.PeerComparisonItem
import com.mindaigle.ui.components.LineChart
import com.mindaigle.ui.components.MultiSeriesLineChart
import com.mindaigle.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociateAnalytics(
    userName: String,
    refreshKey: Int = 0,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var requests by remember { mutableStateOf<List<com.mindaigle.data.remote.dto.AssistanceRequest>>(emptyList()) }
    var timeRange by remember { mutableIntStateOf(7) }
    var trendsData by remember { mutableStateOf<com.mindaigle.data.remote.dto.AnalyticsTrendsResponse?>(null) }
    var trendsLoading by remember { mutableStateOf(true) }
    var selectedStudentIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showStudentPicker by remember { mutableStateOf(false) }
    val studentRepo = remember { StudentRepository() }
    val assistanceRepo = remember { AssistanceRepository() }
    val analyticsRepo = remember { AnalyticsRepository() }
    val scrollState = rememberScrollState()

    // Refresh when refreshKey changes (tab navigation) or on initial load
    LaunchedEffect(refreshKey) {
        // Load data
        studentRepo.getAllStudents()
            .onSuccess { studentsList ->
                students = studentsList
            }
        assistanceRepo.getRequests()
            .onSuccess { requestsList ->
                requests = requestsList
            }
        // Load trends data (with optional selected students for peer comparison)
        val ids = selectedStudentIds.takeIf { it.isNotEmpty() }?.toList()
        analyticsRepo.getTrends(timeRange, ids)
            .onSuccess {
                trendsData = it
                trendsLoading = false
                if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("AssociateAnalytics", "Trends loaded successfully: ${it.stressTrends.size} stress points, ${it.activityTrends.size} activity points")
            }
            .onFailure { error ->
                trendsLoading = false
                android.util.Log.e("AssociateAnalytics", "Failed to load trends: ${error.message}", error)
            }
    }
    
    // Automatic periodic refresh every 20 seconds while screen is active
    LaunchedEffect(Unit) {
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

    // Refresh when time range or selected students change
    LaunchedEffect(timeRange, selectedStudentIds) {
        trendsLoading = true
        studentRepo.getAllStudents()
            .onSuccess { studentsList ->
                students = studentsList
            }
        val ids = selectedStudentIds.takeIf { it.isNotEmpty() }?.toList()
        analyticsRepo.getTrends(timeRange, ids)
            .onSuccess {
                trendsData = it
                trendsLoading = false
                if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("AssociateAnalytics", "Trends refreshed for ${timeRange}d: ${it.stressTrends.size} stress points, ${it.activityTrends.size} activity points")
            }
            .onFailure { error ->
                trendsLoading = false
                android.util.Log.e("AssociateAnalytics", "Failed to refresh trends: ${error.message}", error)
            }
    }

    val displayStudents = if (selectedStudentIds.isEmpty()) students else students.filter { it.id in selectedStudentIds }
    val totalStudents = displayStudents.size
    val avgStress = displayStudents
        .mapNotNull { it.lastCheckin?.stressLevel }
        .takeIf { it.isNotEmpty() }
        ?.average()?.toFloat() ?: 0f
    val activeCheckIns = displayStudents.count { it.lastCheckin != null }
    val highRiskCount = displayStudents.count { it.lastCheckin?.stressLevel != null && it.lastCheckin.stressLevel >= 7 }
    // Show only selected students in peer comparison and charts (dynamic filtering)
    val selectedIdsSet = selectedStudentIds
    val peerComparison = if (selectedIdsSet.isEmpty()) {
        trendsData?.peerComparison
    } else {
        trendsData?.peerComparison?.filter { it.studentId in selectedIdsSet }
    }
    val studentTrends = if (selectedIdsSet.isEmpty()) {
        trendsData?.studentTrends
    } else {
        trendsData?.studentTrends?.filter { it.studentId in selectedIdsSet }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics",
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
                    IconButton(onClick = { onNavigateToTab(8) }) {
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

                // Student multi-select for peer comparison
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Compare students",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showStudentPicker = true }) {
                                Text(
                                    if (selectedStudentIds.isEmpty()) "Select students"
                                    else "${selectedStudentIds.size} selected"
                                )
                            }
                        }
                        if (selectedStudentIds.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                students.filter { it.id in selectedStudentIds }.take(5).forEach { s ->
                                    val label = (s.name?.takeIf { it.isNotEmpty() } ?: "Student ${s.id}").let { if (it.length > 12) it.take(12) + "…" else it }
                                    FilterChip(
                                        selected = true,
                                        onClick = { selectedStudentIds = selectedStudentIds - s.id },
                                        label = { Text(label) },
                                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CalmBlue,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                                if (selectedStudentIds.size > 5) {
                                    Text("+${selectedStudentIds.size - 5}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                TextButton(onClick = { selectedStudentIds = emptySet() }) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
                if (showStudentPicker) {
                    AlertDialog(
                        onDismissRequest = { showStudentPicker = false },
                        title = { Text("Select students for peer comparison") },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                students.forEach { s ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                    Checkbox(
                                        checked = s.id in selectedStudentIds,
                                        onCheckedChange = { checked ->
                                            selectedStudentIds = if (checked) selectedStudentIds + s.id else selectedStudentIds - s.id
                                        }
                                    )
                                    Text(
                                        text = s.name?.takeIf { it.isNotEmpty() } ?: "Student ${s.id}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showStudentPicker = false }) {
                                Text("Done")
                            }
                        }
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

                // Selected students data first (from API peer comparison or fallback from students list); only when we have a selection
                if (displayStudents.isNotEmpty() && selectedStudentIds.isNotEmpty()) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, null, tint = CalmBlue, modifier = Modifier.size(24.dp))
                                Text(
                                    text = "Selected students",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (!peerComparison.isNullOrEmpty())
                                    "Latest data for selected students (any date). Charts below show trends in the selected time range."
                                else
                                    "Latest from students list. For trend charts, students need check-ins in the selected time range (try 14d or 30d).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (!peerComparison.isNullOrEmpty()) {
                                peerComparison.forEach { peer ->
                                    PeerComparisonRow(peer)
                                }
                            } else {
                                // Fallback: show selected students from GET /students (lastCheckin) when trends API has no peer comparison
                                displayStudents.forEach { student ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = student.name?.takeIf { it.isNotEmpty() } ?: "Student ${student.id}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = if (student.lastCheckin != null) "Last check-in: Recent" else "No check-in yet",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                            student.lastCheckin?.stressLevel?.let { s ->
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Stress", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                                    Text(
                                                        text = "$s",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (s >= 7) StatusUrgent else if (s >= 5) StatusOkay else CalmGreen
                                                    )
                                                }
                                            } ?: Text(
                                                text = "—",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Stress Trends Chart (one line per student when students selected, else average)
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
                            text = if (!studentTrends.isNullOrEmpty()) "Stress by student" else "Average Stress Trends",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (trendsLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = StatusUrgent)
                            }
                        } else when {
                            !studentTrends.isNullOrEmpty() -> {
                                val series = studentTrends.map { it.studentName to it.stressByDay }
                                val hasAny = series.any { (_, data) -> data.any { v -> v > 0 } }
                                val dateLabels = trendsData?.dates ?: getDateLabels(timeRange)
                                if (hasAny) {
                                    MultiSeriesLineChart(
                                        series = series,
                                        labels = dateLabels
                                    )
                                } else {
                                    EmptyChartPlaceholder(
                                        message = "No stress data for selected students in this range.",
                                        hint = "Select students above and ensure they have check-ins in the last $timeRange days."
                                    )
                                }
                            }
                            else -> {
                                val stressData = trendsData?.stressTrends ?: emptyList()
                                val dateLabels = trendsData?.dates ?: getDateLabels(timeRange)
                                val hasData = stressData.isNotEmpty() && stressData.any { it > 0 }
                                if (hasData) {
                                    LineChart(
                                        data = stressData,
                                        labels = dateLabels,
                                        color = StatusUrgent
                                    )
                                } else {
                                    EmptyChartPlaceholder(
                                        message = if (trendsData?.meta?.stressDataPoints == 0) "No stress data available" else "No stress data in selected time range",
                                        hint = when {
                                            displayStudents.isNotEmpty() -> "Select students above for per-student lines, or try 14d/30d."
                                            trendsData?.meta?.stressDataPoints == 0 -> "Check-ins will appear here once students start tracking"
                                            else -> "Try selecting a different time range (14d or 30d)"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Check-in Activity Chart (one line per student when students selected, else common)
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
                            text = if (!studentTrends.isNullOrEmpty()) "Check-in activity by student" else "Check-in Activity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (trendsLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CalmBlue)
                            }
                        } else when {
                            !studentTrends.isNullOrEmpty() -> {
                                val series = studentTrends.map { it.studentName to it.activityByDay }
                                val hasAny = series.any { (_, data) -> data.any { v -> v > 0 } }
                                val dateLabels = trendsData?.dates ?: getDateLabels(timeRange)
                                if (hasAny) {
                                    MultiSeriesLineChart(
                                        series = series,
                                        labels = dateLabels
                                    )
                                } else {
                                    EmptyChartPlaceholder(
                                        message = "No check-in activity for selected students in this range.",
                                        hint = "Select students above and ensure they have check-ins in the last $timeRange days."
                                    )
                                }
                            }
                            else -> {
                                val activityData = trendsData?.activityTrends?.map { it.toFloat() } ?: emptyList()
                                val dateLabels = trendsData?.dates ?: getDateLabels(timeRange)
                                val hasData = activityData.isNotEmpty() && activityData.any { it > 0 }
                                if (hasData) {
                                    LineChart(
                                        data = activityData,
                                        labels = dateLabels,
                                        color = CalmBlue
                                    )
                                } else {
                                    EmptyChartPlaceholder(
                                        message = if (trendsData?.meta?.activityDataPoints == 0) "No activity data available" else "No activity data in selected time range",
                                        hint = when {
                                            displayStudents.isNotEmpty() -> "Select students above for per-student lines, or try 14d/30d."
                                            trendsData?.meta?.activityDataPoints == 0 -> "Check-ins will appear here once students start tracking"
                                            else -> "Try selecting a different time range (14d or 30d)"
                                        }
                                    )
                                }
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
                            text = getAnalyticsInsight(displayStudents, requests, avgStress),
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
private fun EmptyChartPlaceholder(message: String, hint: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📊",
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
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
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0.2f),
                            color.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
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

@Composable
fun PeerComparisonRow(peer: PeerComparisonItem) {
    val stress = peer.lastStress ?: peer.latestStressAny
    val mood = peer.lastMood ?: peer.latestMoodAny
    val inRangeLabel = if (peer.checkinCount == 0) "No check-ins in range" else "${peer.checkinCount} check-ins in range"
    val dateLabel = peer.latestCheckinDate?.let { "Latest: $it" } ?: ""
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.studentName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = inRangeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (dateLabel.isNotEmpty()) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stress?.let { s ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Stress", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = "$s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (s >= 7) StatusUrgent else if (s >= 5) StatusOkay else CalmGreen
                        )
                    }
                }
                mood?.let { m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mood", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = "$m/5",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CalmBlue
                        )
                    }
                }
                if (stress == null && mood == null) {
                    Text(
                        text = "No check-ins yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
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
    requests: List<com.mindaigle.data.remote.dto.AssistanceRequest>,
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

