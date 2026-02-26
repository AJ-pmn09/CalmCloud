package com.mindaigle.ui.screens.expert

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
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.data.remote.dto.Student
import com.mindaigle.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertStudents(
    userName: String,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var notificationCount by remember { mutableIntStateOf(0) }
    val studentRepo = remember { StudentRepository() }

    LaunchedEffect(Unit) {
        // Initial load
        studentRepo.getAllStudents()
            .onSuccess { studentsList ->
                students = studentsList
                isLoading = false
            }
            .onFailure {
                isLoading = false
            }
        
        // Automatic periodic refresh every 15 seconds while screen is active
        while (true) {
            kotlinx.coroutines.delay(15000) // 15 seconds
            studentRepo.getAllStudents()
                .onSuccess { studentsList ->
                    students = studentsList
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
                        text = "Expert Portal",
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
                    Box {
                        IconButton(onClick = { onNavigateToTab(2) }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Reports")
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
                // All Students Card
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
                            text = "All Students (${students.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
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
                            Text(
                                text = "No students found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        } else {
                            students.forEach { student ->
                                ExpertStudentRow(student = student)
                                if (student != students.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
fun ExpertStudentRow(student: Student) {
    val status = when {
        student.lastCheckin?.stressLevel != null && student.lastCheckin.stressLevel >= 8 -> "Critical" to StatusUrgent
        student.lastCheckin?.stressLevel != null && student.lastCheckin.stressLevel >= 5 -> "Monitor" to StatusOkay
        else -> "Good" to CalmGreen
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name ?: "Unknown Student",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = student.email,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${student.observationCount} observations • Last activity: ${formatLastActivity(student)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        // Status Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = status.second.copy(alpha = 0.2f)
        ) {
            Text(
                text = status.first,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = status.second,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

fun formatLastActivity(student: Student): String {
    return if (student.lastCheckin != null || student.observationCount > 0) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy, h:mm:ss a", Locale.getDefault())
        dateFormat.format(calendar.time)
    } else {
        "No activity"
    }
}

