package com.mindaigle.ui.screens.associate

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.data.repository.ScreenerRepository
import com.mindaigle.data.remote.dto.Student
import com.mindaigle.data.remote.dto.ScreenerInstanceWithScore
import com.mindaigle.ui.components.ProfilePicture
import com.mindaigle.ui.components.ProfilePictureShape
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociateStudents(
    userName: String,
    refreshKey: Int = 0,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var notificationCount by remember { mutableIntStateOf(1) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var screenerReport by remember { mutableStateOf<Map<String, ScreenerInstanceWithScore>?>(null) }
    var screenerReportLoading by remember { mutableStateOf(false) }
    var assignLoading by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showFhirExportDialog by remember { mutableStateOf(false) }
    val studentRepo = remember { StudentRepository() }
    val screenerRepo = remember { ScreenerRepository() }
    val scope = rememberCoroutineScope()

    // Refresh when refreshKey changes (tab navigation) or on initial load
    LaunchedEffect(refreshKey) {
        // Load data
        studentRepo.getAllStudents()
            .onSuccess { studentsList ->
                students = studentsList
                isLoading = false
            }
            .onFailure {
                isLoading = false
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
        }
    }

    val totalStudents = students.size
    val needAttention = students.count { it.lastCheckin?.stressLevel != null && it.lastCheckin.stressLevel >= 5 }
    val critical = students.count { it.lastCheckin?.stressLevel != null && it.lastCheckin.stressLevel >= 8 }

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
                        text = "Associate Portal",
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
                    // Notification bell with badge
                    Box {
                        IconButton(onClick = { onNavigateToTab(1) }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                        if (notificationCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-8).dp)
                            ) {
                                Text("$notificationCount")
                            }
                        }
                    }
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
                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        value = "$totalStudents",
                        label = "Total Students",
                        color = CalmPurple,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        value = "$needAttention",
                        label = "Need Attention",
                        color = StatusOkay,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        value = "$critical",
                        label = "Critical",
                        color = StatusUrgent,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Student Wellness Monitoring Section
                Text(
                    text = "Student Wellness Monitoring",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                        color = CalmBlue,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    }
                } else if (students.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No students found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    students.forEach { student ->
                        StudentWellnessCard(
                            student = student,
                            onClick = { selectedStudent = student }
                        )
                    }
                }
            }
        }
        val snackbarHostState = remember { SnackbarHostState() }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                snackbarMessage = null
            }
        }
        if (showFhirExportDialog && selectedStudent != null) {
            AssociateFhirExportDialog(
                studentName = selectedStudent!!.name ?: "Student ${selectedStudent!!.id}",
                onDismiss = { showFhirExportDialog = false },
                onExport = { startDate, endDate ->
                    scope.launch {
                        studentRepo.exportFHIRDataForStudent(selectedStudent!!.id, startDate, endDate)
                            .onSuccess {
                                snackbarMessage = "FHIR data exported successfully"
                                showFhirExportDialog = false
                            }
                            .onFailure {
                                snackbarMessage = "Export failed: ${it.message}"
                            }
                    }
                }
            )
        }
        selectedStudent?.let { student ->
            StudentScreenerDrawer(
                student = student,
                screenerReport = screenerReport,
                screenerReportLoading = screenerReportLoading,
                assignLoading = assignLoading,
                onDismiss = {
                    selectedStudent = null
                    screenerReport = null
                },
                onExportFHIR = { showFhirExportDialog = true },
                onLoadReport = {
                    screenerReportLoading = true
                    scope.launch {
                        screenerRepo.getStudentReport(student.id)
                            .onSuccess { screenerReport = it.latestByType }
                            .onFailure { snackbarMessage = it.message }
                        screenerReportLoading = false
                    }
                },
                onAssign = { type ->
                    assignLoading = true
                    scope.launch {
                        screenerRepo.createInstance(student.id, type, overrideFrequency = false)
                            .onSuccess {
                                snackbarMessage = when (type.trim().lowercase()) {
                                    "phq9" -> "PHQ-9 assigned to student."
                                    "gad7" -> "GAD-7 assigned to student."
                                    else -> "${type.uppercase()} assigned."
                                }
                                screenerRepo.getStudentReport(student.id).onSuccess { screenerReport = it.latestByType }
                            }
                            .onFailure {
                                snackbarMessage = it.message ?: "Failed. (Maybe already completed in last 2 weeks?)"
                            }
                        assignLoading = false
                    }
                }
            )
        }
    }
}

@Composable
fun SummaryCard(
    value: String,
    label: String,
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
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun StudentWellnessCard(student: Student, onClick: () -> Unit = {}) {
    val stressLevel = student.lastCheckin?.stressLevel
    val status = when {
        stressLevel != null && stressLevel >= 8 -> "Critical" to StatusUrgent
        stressLevel != null && stressLevel >= 5 -> "Monitor" to StatusOkay
        else -> "Good" to CalmGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfilePicture(
                imageUrl = student.profilePictureUrl,
                userName = student.name ?: "?",
                size = 48.dp,
                shape = ProfilePictureShape.Circle
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name ?: "Unknown Student",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (student.lastCheckin != null) {
                        // Show relative time or recent
                        "Last check-in: Recent"
                    } else if (student.observationCount > 0) {
                        "Has check-in history"
                    } else {
                        "No check-in yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            // Status Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = status.second.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (status.first == "Good") Icons.Default.CheckCircle else Icons.Default.Warning,
                        null,
                        tint = status.second,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = status.first,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = status.second
                    )
                }
            }
        }
    }
}

@Composable
fun AssociateFhirExportDialog(
    studentName: String,
    onDismiss: () -> Unit,
    onExport: (startDate: String, endDate: String) -> Unit
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export FHIR data", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Export $studentName's health data as FHIR R4 (date range)", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (startDate.isNotBlank() && endDate.isNotBlank()) onExport(startDate, endDate)
                },
                enabled = startDate.isNotBlank() && endDate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
            ) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreenerDrawer(
    student: Student,
    screenerReport: Map<String, ScreenerInstanceWithScore>?,
    screenerReportLoading: Boolean,
    assignLoading: Boolean,
    onDismiss: () -> Unit,
    onExportFHIR: () -> Unit = {},
    onLoadReport: () -> Unit,
    onAssign: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = student.name ?: "Student ${student.id}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Clinical screeners (PHQ-9 / GAD-7)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAssign("phq9") },
                        enabled = !assignLoading,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                    ) {
                        if (assignLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Assign PHQ-9")
                    }
                    Button(
                        onClick = { onAssign("gad7") },
                        enabled = !assignLoading,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CalmPurple)
                    ) {
                        Text("Assign GAD-7")
                    }
                }
                OutlinedButton(
                    onClick = onExportFHIR,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export FHIR data")
                }
                OutlinedButton(
                    onClick = onLoadReport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !screenerReportLoading
                ) {
                    if (screenerReportLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("View latest screening results")
                    }
                }
                screenerReport?.let { byType ->
                    if (byType.isNotEmpty()) {
                        Text(
                            text = "Latest results",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        byType.forEach { (type, row) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = (if (row.positive == true) StatusSupport else CalmGreen).copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = type.uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = "Score: ${row.total ?: "--"}", style = MaterialTheme.typography.bodySmall)
                                        Text(text = row.severity ?: "--", style = MaterialTheme.typography.bodySmall)
                                        if (row.positive == true) {
                                            Text(text = "Positive", style = MaterialTheme.typography.labelSmall, color = StatusUrgent)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No screening results yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

