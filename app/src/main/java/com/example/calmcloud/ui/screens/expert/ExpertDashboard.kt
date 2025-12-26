package com.example.calmcloud.ui.screens.expert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
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
import com.example.calmcloud.data.repository.StudentRepository
import com.example.calmcloud.data.remote.dto.Student
import com.example.calmcloud.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertDashboard(userName: String) {
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var notificationCount by remember { mutableIntStateOf(0) }
    val studentRepo = remember { StudentRepository() }

    LaunchedEffect(Unit) {
        studentRepo.getAllStudents()
            .onSuccess { studentsList ->
                students = studentsList
                isLoading = false
            }
            .onFailure {
                isLoading = false
            }
    }

    val totalStudents = students.size
    val activeToday = students.count { it.observationCount > 0 }
    val needAttention = students.count { 
        it.lastCheckin?.stressLevel != null && 
        it.lastCheckin.stressLevel >= 5 && 
        it.lastCheckin.stressLevel < 8 
    }
    val criticalCases = students.count { 
        it.lastCheckin?.stressLevel != null && 
        it.lastCheckin.stressLevel >= 8 
    }

    val avgStress = students
        .mapNotNull { it.lastCheckin?.stressLevel }
        .takeIf { it.isNotEmpty() }
        ?.average()?.toFloat() ?: 0f

    val avgEmotional = students
        .mapNotNull { it.lastCheckin?.emotionIntensity }
        .takeIf { it.isNotEmpty() }
        ?.average()?.toFloat() ?: 0f

    val studentsRequiringAttention = students.filter { 
        it.lastCheckin?.stressLevel != null && 
        it.lastCheckin.stressLevel >= 5 
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
                        text = "Expert Portal",
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
                    Box {
                        IconButton(onClick = { /* Notifications */ }) {
                            Icon(Icons.Default.Notifications, null)
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
                // Summary Cards - 2x2 Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpertSummaryCard(
                            title = "Total Students",
                            value = "$totalStudents",
                            icon = Icons.Default.Person,
                            gradientColors = listOf(CalmPurple, CalmPurple.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f)
                        )
                        ExpertSummaryCard(
                            title = "Active Today",
                            value = "$activeToday",
                            icon = Icons.Default.Star,
                            gradientColors = listOf(CalmGreen, CalmGreen.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpertSummaryCard(
                            title = "Need Attention",
                            value = "$needAttention",
                            icon = Icons.Default.Warning,
                            gradientColors = listOf(StatusOkay, StatusOkay.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f)
                        )
                        ExpertSummaryCard(
                            title = "Critical Cases",
                            value = "$criticalCases",
                            icon = Icons.Default.Warning,
                            gradientColors = listOf(StatusUrgent, StatusUrgent.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Population Averages Card
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
                            text = "Population Averages",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        AverageProgressBar(
                            label = "Average Stress Level",
                            value = avgStress,
                            maxValue = 10f,
                            color = StatusOkay
                        )
                        AverageProgressBar(
                            label = "Average Emotional Score",
                            value = avgEmotional,
                            maxValue = 10f,
                            color = StatusOkay
                        )
                    }
                }

                // Students Requiring Attention Card
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
                            text = "Students Requiring Attention",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (studentsRequiringAttention.isEmpty()) {
                            Text(
                                text = "No students currently require attention.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        } else {
                            studentsRequiringAttention.forEach { student ->
                                StudentAttentionRow(student = student)
                                if (student != studentsRequiringAttention.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }

                // FHIR Compliance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
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
            }
        }
    }
}

@Composable
fun ExpertSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(gradientColors),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun AverageProgressBar(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color
) {
    val progress = (value / maxValue).coerceIn(0f, 1f)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
            Text(
                text = String.format("%.1f/%.0f", value, maxValue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { progress },
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
fun StudentAttentionRow(student: Student) {
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
                text = "Last check-in: ${formatDateForExpert(student)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Button(
            onClick = { /* Monitor action */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = StatusOkay.copy(alpha = 0.2f),
                contentColor = StatusOkay
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Monitor")
        }
    }
}

fun formatDateForExpert(student: Student): String {
    // For now, show recent if there's a check-in
    return if (student.lastCheckin != null) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        dateFormat.format(calendar.time)
    } else {
        "No check-in"
    }
}

