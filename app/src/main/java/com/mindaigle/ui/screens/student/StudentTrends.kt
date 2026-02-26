package com.mindaigle.ui.screens.student

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindaigle.data.remote.dto.StudentFHIRData
import com.mindaigle.data.remote.dto.TrendsDataResponse
import com.mindaigle.data.remote.dto.FHIRExportResponse
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.ui.components.LineChart
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTrends(
    userName: String,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var selectedMetric by remember { mutableStateOf("Mood") } // "Mood" or "Stress"
    var timeRange by remember { mutableIntStateOf(7) } // 7, 14, or 30 days
    var periodType by remember { mutableStateOf("Days") } // "Days", "Weeks", "Months", "Year"
    var selectedStartDate by remember { mutableStateOf<String?>(null) }
    var selectedEndDate by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showFhirExportDialog by remember { mutableStateOf(false) }
    var trendsData by remember { mutableStateOf<com.mindaigle.data.remote.dto.TrendsDataResponse?>(null) }
    val repository = remember { StudentRepository() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load trends data when metric, time range, or period type changes
    LaunchedEffect(selectedMetric, timeRange, periodType, selectedStartDate, selectedEndDate) {
        try {
            val daysToLoad = when (periodType) {
                "Days" -> timeRange
                "Weeks" -> timeRange * 7
                "Months" -> timeRange * 30
                "Year" -> 365
                else -> timeRange
            }
            repository.getTrendsData(daysToLoad)
                .onSuccess { response ->
                    trendsData = response
                    if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("StudentTrends", "Trends data loaded: ${response.checkins.size} checkins, ${response.activities.size} activities")
                }
                .onFailure { error ->
                    android.util.Log.e("StudentTrends", "Failed to load trends data: ${error.message}", error)
                }
        } catch (e: Throwable) {
            android.util.Log.e("StudentTrends", "Load error", e)
        }
    }

    // Automatic periodic refresh every 30 seconds while screen is active
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30000)
            try {
                val daysToLoad = when (periodType) {
                    "Days" -> timeRange
                    "Weeks" -> timeRange * 7
                    "Months" -> timeRange * 30
                    "Year" -> 365
                    else -> timeRange
                }
                repository.getTrendsData(daysToLoad)
                    .onSuccess { response -> trendsData = response }
                    .onFailure { e -> android.util.Log.e("StudentTrends", "Refresh failed: ${e.message}", e) }
            } catch (e: Throwable) {
                android.util.Log.e("StudentTrends", "Refresh error", e)
            }
        }
    }

    // Calculate chart data based on period type
    val daysForChart = when (periodType) {
        "Days" -> timeRange
        "Weeks" -> timeRange * 7
        "Months" -> timeRange * 30
        "Year" -> 365
        else -> timeRange
    }
    
    val chartData = getChartData(trendsData, selectedMetric, daysForChart, selectedStartDate, selectedEndDate)
    val hasNoCheckinData = trendsData?.checkins.isNullOrEmpty() || chartData.all { it == 5f }
    val average = if (chartData.isEmpty()) 5f else chartData.average().toFloat()
    val checkIns = chartData.size
    val trend = if (chartData.size < 2) "Stable" else calculateTrend(chartData)
    val chartScale by animateFloatAsState(
        targetValue = if (trendsData != null) 1f else 0.95f,
        animationSpec = tween(400),
        label = "chartScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                    IconButton(onClick = { onNavigateToTab(0) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToTab(7) }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = TextPrimary)
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
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mood/Stress Toggle - iOS Style
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceLight)
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedMetric == "Mood",
                                onClick = { selectedMetric = "Mood" },
                                label = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("😊", style = MaterialTheme.typography.bodyLarge)
                                        Text("Mood")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CalmBlue,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            FilterChip(
                                selected = selectedMetric == "Stress",
                                onClick = { selectedMetric = "Stress" },
                                label = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🧠", style = MaterialTheme.typography.bodyLarge)
                                        Text("Stress")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CalmPurple,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // Period Type Selector (Days/Weeks/Months/Year)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Days", "Weeks", "Months", "Year").forEach { period ->
                            FilterChip(
                                selected = periodType == period,
                                onClick = {
                                    periodType = period
                                    when (period) {
                                        "Days" -> timeRange = 30
                                        "Weeks" -> timeRange = 12
                                        "Months" -> timeRange = 12
                                        "Year" -> timeRange = 12
                                    }
                                },
                                label = { Text(period) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CalmBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }

                // Time Range Selector (based on period type)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    when (periodType) {
                        "Days" -> {
                            listOf(7, 14, 30).forEach { days ->
                                FilterChip(
                                    selected = timeRange == days,
                                    onClick = { timeRange = days },
                                    label = { Text("${days}d") },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CalmBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceLight,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                        "Weeks" -> {
                            listOf(4, 8, 12).forEach { weeks ->
                                FilterChip(
                                    selected = timeRange == weeks,
                                    onClick = { timeRange = weeks },
                                    label = { Text("${weeks}w") },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CalmBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceLight,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                        "Months" -> {
                            listOf(3, 6, 12).forEach { months ->
                                FilterChip(
                                    selected = timeRange == months,
                                    onClick = { timeRange = months },
                                    label = { Text("${months}m") },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CalmBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceLight,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                        "Year" -> {
                            FilterChip(
                                selected = true,
                                onClick = { },
                                label = { Text("Full Year") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Custom Date Range Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedStartDate != null && selectedEndDate != null) "Custom Range" else "Select Date Range")
                    }
                    OutlinedButton(
                        onClick = { showFhirExportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export FHIR")
                    }
                }

                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        label = "Average",
                        value = when (selectedMetric) {
                            "Mood" -> String.format("%.1f (%s)", average, moodLabel(average))
                            "Stress" -> String.format("%.1f (%s)", average, stressLevelLabelTrends(average))
                            else -> String.format("%.1f", average)
                        },
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

                // Mood/Stress Over Time Chart - iOS Style
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(chartScale),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        if (selectedMetric == "Mood") CalmBlue.copy(alpha = 0.05f) else CalmPurple.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedMetric == "Mood") "😊" else "🧠",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${selectedMetric} Over Time",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            if (hasNoCheckinData) {
                                Text(
                                    text = "No check-in data yet. Complete check-ins on the Check-in tab to see your $selectedMetric over time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                LineChart(
                                    data = chartData,
                                    labels = getDateLabels(daysForChart, periodType, selectedStartDate, selectedEndDate),
                                    color = if (selectedMetric == "Mood") CalmBlue else CalmPurple,
                                    showArea = false
                                )
                            }
                        }
                    }
                }

                // Insight Card - iOS Style with vibrant gradient
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        CalmBlue.copy(alpha = 0.12f),
                                        SurfaceLight
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CalmBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⭐",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Insight",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (hasNoCheckinData) "Complete check-ins to see your $selectedMetric insights here." else getInsight(selectedMetric, average),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                                )
                            }
                        }
                    }
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DateRangePickerDialog(
                onDismiss = { showDatePicker = false },
                onConfirm = { startDate, endDate ->
                    selectedStartDate = startDate
                    selectedEndDate = endDate
                    showDatePicker = false
                    // Reload data with custom date range
                    scope.launch {
                        val days = calculateDaysBetween(startDate, endDate)
                        repository.getTrendsData(days)
                            .onSuccess { response ->
                                trendsData = response
                            }
                    }
                }
            )
        }

        // FHIR Export Dialog
        if (showFhirExportDialog) {
            FHIRExportDialog(
                onDismiss = { showFhirExportDialog = false },
                onExport = { startDate, endDate ->
                    scope.launch {
                        repository.exportFHIRData(startDate, endDate)
                            .onSuccess {
                                snackbarHostState.showSnackbar("FHIR data exported successfully!")
                                showFhirExportDialog = false
                            }
                            .onFailure {
                                snackbarHostState.showSnackbar("Failed to export: ${it.message}")
                            }
                    }
                }
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-01-01") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-01-31") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (startDate.isNotBlank() && endDate.isNotBlank()) {
                        onConfirm(startDate, endDate)
                    }
                },
                enabled = startDate.isNotBlank() && endDate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FHIRExportDialog(
    onDismiss: () -> Unit,
    onExport: (String, String) -> Unit
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export FHIR Data", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Export your health data as FHIR R4 resources")
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-01-01") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-01-31") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (startDate.isNotBlank() && endDate.isNotBlank()) {
                        onExport(startDate, endDate)
                    }
                },
                enabled = startDate.isNotBlank() && endDate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun calculateDaysBetween(startDate: String, endDate: String): Int {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = format.parse(startDate) ?: return 30
        val end = format.parse(endDate) ?: return 30
        val diff = end.time - start.time
        (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    } catch (e: Exception) {
        30
    }
}

@Composable
fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val (accentColor, icon) = when (label) {
        "Average" -> CalmBlue to Icons.AutoMirrored.Filled.ShowChart
        "Check-ins" -> CalmGreen to Icons.Default.CheckCircle
        "Trend" -> StatusOkay to Icons.Default.Info
        else -> TextSecondary to Icons.Default.Info
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = when (label) {
                            "Average" -> listOf(CalmBlue.copy(alpha = 0.12f), CalmBlue.copy(alpha = 0.05f))
                            "Check-ins" -> listOf(CalmGreen.copy(alpha = 0.12f), CalmGreen.copy(alpha = 0.05f))
                            "Trend" -> listOf(StatusOkay.copy(alpha = 0.12f), StatusOkay.copy(alpha = 0.05f))
                            else -> listOf(SurfaceLight, Color.White)
                        }
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(20.dp), tint = accentColor)
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                if (label == "Trend") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val trendEmoji = when (value.lowercase()) {
                            "improving" -> "📈"
                            "declining" -> "📉"
                            else -> "➡️"
                        }
                        Text(
                            text = trendEmoji,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (value.lowercase()) {
                                "improving" -> CalmGreen
                                "declining" -> CalmPurple
                                else -> TextSecondary
                            },
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (label) {
                            "Average" -> CalmBlue
                            "Check-ins" -> CalmGreen
                            else -> TextPrimary
                        }
                    )
                }
            }
        }
    }
}

fun getChartData(
    data: com.mindaigle.data.remote.dto.TrendsDataResponse?, 
    metric: String, 
    days: Int,
    startDate: String? = null,
    endDate: String? = null
): List<Float> {
    if (data == null) return List(days) { if (metric == "Mood") 5f else 5f }
    
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Generate date range - use custom dates if provided
    val dates = if (startDate != null && endDate != null) {
        try {
            val start = dateFormat.parse(startDate) ?: return List(days) { if (metric == "Mood") 5f else 5f }
            val end = dateFormat.parse(endDate) ?: return List(days) { if (metric == "Mood") 5f else 5f }
            val diff = ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt()
            (0..diff).map { dayOffset ->
                calendar.time = start
                calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
                dateFormat.format(calendar.time)
            }
        } catch (e: Exception) {
            List(days) { if (metric == "Mood") 5f else 5f }
        }
    } else {
        (0 until days).map { daysAgo ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            dateFormat.format(calendar.time)
        }.reversed()
    }
    
    // Create a map of date -> value for quick lookup
    // ONLY use check-ins (no activity data)
    val dataMap: Map<String, Float> = if (metric == "Mood") {
        // Use check-ins for mood: prefer mood_rating, fallback to mood (integer)
        data.checkins
            .filterNotNull()
            .filter { it.date != null }
            .associate { checkin ->
                val date = checkin.date!!
                val value = checkin.moodRating?.toFloat() ?: checkin.mood?.toFloat()
                date to (value ?: 5f)
            }
    } else {
        // Use check-ins for stress: stress_level only
        data.checkins
            .filterNotNull()
            .filter { it.date != null }
            .mapNotNull { checkin ->
                checkin.date?.let { date ->
                    checkin.stressLevel?.toFloat()?.let { value ->
                        date to value
                    }
                }
            }
            .associate { it.first to it.second }
    }
    
    // Map each date to its value, or default
    return dates.map { dateStr ->
        dataMap[dateStr] ?: (if (metric == "Mood") 5f else 5f)
    }
}

fun getDateLabels(
    days: Int, 
    periodType: String = "Days",
    startDate: String? = null,
    endDate: String? = null
): List<String> {
    val calendar = Calendar.getInstance()
    val dateFormat = when (periodType) {
        "Weeks" -> SimpleDateFormat("MMM dd", Locale.getDefault())
        "Months" -> SimpleDateFormat("MMM", Locale.getDefault())
        "Year" -> SimpleDateFormat("MMM yyyy", Locale.getDefault())
        else -> SimpleDateFormat("MMM dd", Locale.getDefault())
    }
    
    return if (startDate != null && endDate != null) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = inputFormat.parse(startDate) ?: return emptyList()
            val end = inputFormat.parse(endDate) ?: return emptyList()
            val diff = ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt()
            (0..diff).map { dayOffset ->
                calendar.time = start
                calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
                dateFormat.format(calendar.time)
            }
        } catch (e: Exception) {
            emptyList()
        }
    } else {
        (0 until days).map { daysAgo ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            dateFormat.format(calendar.time)
        }.reversed()
    }
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

fun wellnessScoreLabel(score: Float): String = when {
    score >= 80f -> "Excellent"
    score in 60f..79f -> "Good"
    score in 40f..59f -> "Fair"
    score in 20f..39f -> "Needs Attention"
    else -> "Critical"
}

fun stressLevelLabelTrends(value: Float): String = when {
    value <= 2f -> "Very Low"
    value <= 4f -> "Low"
    value <= 6f -> "Moderate"
    value <= 8f -> "High"
    else -> "Very High"
}

fun moodLabel(value: Float): String = when {
    value >= 7f -> "Excellent"
    value >= 5f -> "Good"
    value >= 3f -> "Fair"
    else -> "Needs Attention"
}

fun getInsight(metric: String, average: Float): String {
    return when (metric) {
        "Mood" -> when {
            average >= 7 -> "Your mood has been consistently positive (${moodLabel(average)})! Keep up the great work with self-care."
            average >= 5 -> "Your mood is stable (${moodLabel(average)}). Continue practicing self-care activities."
            else -> "Your mood could use some support (${moodLabel(average)}). Consider trying breathing exercises or talking to someone."
        }
        "Stress" -> when {
            average <= 3 -> "Your stress levels are low (${stressLevelLabelTrends(average)}) — excellent! Keep up the healthy habits."
            average <= 6 -> "Your stress is moderate (${stressLevelLabelTrends(average)}). Remember to take breaks and practice relaxation."
            else -> "Your stress levels are elevated (${stressLevelLabelTrends(average)}). Consider trying stress-reduction techniques."
        }
        else -> "Keep tracking your wellness journey!"
    }
}

