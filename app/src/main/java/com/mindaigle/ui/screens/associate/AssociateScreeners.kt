package com.mindaigle.ui.screens.associate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.mindaigle.data.remote.dto.ScreenerInstanceWithScore
import com.mindaigle.data.remote.dto.Student
import com.mindaigle.data.repository.ScreenerRepository
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociateScreeners(
    userName: String,
    refreshKey: Int = 0,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var reportData by remember { mutableStateOf<com.mindaigle.data.remote.dto.ScreenerStudentReportData?>(null) }
    var reportLoading by remember { mutableStateOf(false) }
    var assignLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showStudentPicker by remember { mutableStateOf(false) }
    val studentRepo = remember { StudentRepository() }
    val screenerRepo = remember { ScreenerRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshKey) {
        studentRepo.getAllStudents().onSuccess { students = it }
    }

    LaunchedEffect(selectedStudent) {
        selectedStudent?.let { student ->
            reportLoading = true
            screenerRepo.getStudentReport(student.id)
                .onSuccess { reportData = it }
                .onFailure { message = it.message }
            reportLoading = false
        } ?: run { reportData = null }
    }

    LaunchedEffect(message) {
        message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            message = null
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
                        text = "Clinical Screeners",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Student selector
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
                            Icon(Icons.Default.Person, null, tint = CalmBlue, modifier = Modifier.size(24.dp))
                            Text(
                                text = "Select student",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { showStudentPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedStudent?.name?.takeIf { it.isNotEmpty() }
                                    ?: selectedStudent?.let { "Student ${it.id}" }
                                    ?: "Choose a student to view PHQ-9 / GAD-7 results"
                            )
                        }
                    }
                }

                selectedStudent?.let { student ->
                    // Assign PHQ-9 / GAD-7
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
                            Text(
                                text = "Assign screener",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        assignLoading = true
                                        scope.launch {
                                            screenerRepo.createInstance(student.id, "phq9", false)
                                                .onSuccess {
                                                    message = "PHQ-9 assigned."
                                                    screenerRepo.getStudentReport(student.id).onSuccess { reportData = it }
                                                }
                                                .onFailure { message = it.message }
                                            assignLoading = false
                                        }
                                    },
                                    enabled = !assignLoading,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                                ) {
                                    if (assignLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    else Text("Assign PHQ-9")
                                }
                                Button(
                                    onClick = {
                                        assignLoading = true
                                        scope.launch {
                                            screenerRepo.createInstance(student.id, "gad7", false)
                                                .onSuccess {
                                                    message = "GAD-7 assigned."
                                                    screenerRepo.getStudentReport(student.id).onSuccess { reportData = it }
                                                }
                                                .onFailure { message = it.message }
                                            assignLoading = false
                                        }
                                    },
                                    enabled = !assignLoading,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CalmPurple)
                                ) {
                                    Text("Assign GAD-7")
                                }
                            }
                        }
                    }

                    // Results: latest by type + history
                    if (reportLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CalmBlue)
                        }
                    } else if (reportData != null) {
                        val data = reportData!!
                        // Latest results (PHQ-9 / GAD-7)
                        if (data.latestByType.isNotEmpty()) {
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
                                            Icon(Icons.Default.Star, null, tint = CalmBlue, modifier = Modifier.size(24.dp))
                                            Text(
                                                text = "Latest results",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        data.latestByType.forEach { (type, row) ->
                                            ScreenerResultRow(row, type)
                                        }
                                    }
                                }
                            }

                            // History
                            if (data.history.isNotEmpty()) {
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
                                            Icon(Icons.Default.DateRange, null, tint = CalmBlue, modifier = Modifier.size(24.dp))
                                            Text(
                                                text = "History",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        data.history.forEach { row ->
                                            ScreenerResultRow(row, row.screenerType)
                                        }
                                    }
                                }
                            }

                            if (data.latestByType.isEmpty() && data.history.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No screening results yet for this student. Assign PHQ-9 or GAD-7 above.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        } else if (selectedStudent != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Could not load results. Check connection and try again.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

        if (showStudentPicker) {
            AlertDialog(
                onDismissRequest = { showStudentPicker = false },
                title = { Text("Select student") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        students.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedStudent = s
                                        showStudentPicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.name?.takeIf { it.isNotEmpty() } ?: "Student ${s.id}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStudentPicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun ScreenerResultRow(row: ScreenerInstanceWithScore, typeLabel: String) {
    val dateStr = row.completedAt?.let { str ->
        try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            fmt.parse(str)?.let { SimpleDateFormat("MMM d, yyyy", Locale.US).format(it) } ?: str.take(10)
        } catch (_: Exception) {
            str.take(10)
        }
    } ?: "—"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (row.positive == true) StatusSupport.copy(alpha = 0.15f) else CalmGreen.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeLabel.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score: ${row.total ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = row.severity ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (row.positive == true) {
                    Text(
                        text = "Positive",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusUrgent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
