package com.mindaigle.ui.screens.associate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mindaigle.data.repository.CommunicationRepository
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.data.remote.dto.Student
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociateCommunications(
    userName: String,
    refreshKey: Int = 0,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var showSendDialog by remember { mutableStateOf(false) }
    var communications by remember { mutableStateOf<List<com.mindaigle.data.remote.dto.SentMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val commRepo = remember { CommunicationRepository() }

    // Refresh when refreshKey changes (tab navigation) or on initial load
    LaunchedEffect(refreshKey) {
        // Load data
        loadSentMessages(commRepo, scope) { result ->
            result.onSuccess { communications = it.first }
                .onFailure { errorMessage = it.message }
        }
    }
    
    // Automatic periodic refresh every 20 seconds while screen is active
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(20000) // 20 seconds
            loadSentMessages(commRepo, scope) { result ->
                result.onSuccess { communications = it.first }
                    .onFailure { errorMessage = it.message }
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
                        text = "Communications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { showSendDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Send message", tint = CalmBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CalmBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Total Messages Sent",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${communications.size}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Messages List
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CalmBlue)
                    }
                } else if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatusUrgent.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = errorMessage ?: "Error loading messages",
                            modifier = Modifier.padding(16.dp),
                            color = StatusUrgent
                        )
                    }
                } else if (communications.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "No messages",
                                modifier = Modifier.size(48.dp),
                                tint = TextSecondary
                            )
                            Text(
                                text = "No messages sent yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "Tap the + button to send your first message",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    communications.forEach { message ->
                        CommunicationCard(message)
                    }
                }
            }
        }
    }

    // Send Message Dialog
    if (showSendDialog) {
        SendMessageDialog(
            onDismiss = { showSendDialog = false },
            onSend = { scope.launch {
                loadSentMessages(commRepo, scope) { result ->
                    result.onSuccess { communications = it.first }
                        .onFailure { errorMessage = it.message }
                }
            }}
        )
    }
}

@Composable
fun CommunicationCard(message: com.mindaigle.data.remote.dto.SentMessage) {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    val date = try {
        val parsedDate = inputFormat.parse(message.createdAt)
        if (parsedDate != null) {
            outputFormat.format(parsedDate)
        } else {
            message.createdAt
        }
    } catch (e: Exception) {
        // Try ISO format without milliseconds
        try {
            val altFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val parsedDate = altFormat.parse(message.createdAt)
            if (parsedDate != null) {
                outputFormat.format(parsedDate)
            } else {
                message.createdAt
            }
        } catch (e2: Exception) {
            message.createdAt
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (message.recipientType) {
                            "student" -> message.recipientName ?: "Student"
                            "cohort" -> message.recipientCohort ?: "Cohort"
                            "all" -> "All Students"
                            else -> "Unknown"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                PriorityBadge(message.priority)
            }
            
            if (!message.subject.isNullOrBlank()) {
                Text(
                    text = message.subject ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (message.emergencyOverride) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusUrgent.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Emergency Override",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusUrgent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (!message.parentVisible) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TextSecondary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Private",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (color, text) = when (priority.lowercase()) {
        "urgent" -> StatusUrgent to "Urgent"
        "high" -> StatusSupport to "High"
        "normal" -> CalmBlue to "Normal"
        "low" -> CalmGreen to "Low"
        else -> TextSecondary to priority
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SendMessageDialog(
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    var recipientType by remember { mutableStateOf("student") }
    var selectedStudentId by remember { mutableIntStateOf(0) }
    var recipientCohort by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("normal") }
    var parentVisible by remember { mutableStateOf(true) }
    var emergencyOverride by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val commRepo = remember { CommunicationRepository() }
    val studentRepo = remember { StudentRepository() }

    LaunchedEffect(Unit) {
        studentRepo.getAllStudents()
            .onSuccess { students = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Send Message",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Recipient Type
                Text(text = "Recipient Type:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("student", "cohort", "all").forEach { type ->
                        FilterChip(
                            selected = recipientType == type,
                            onClick = { recipientType = type },
                            label = { Text(type.replaceFirstChar { it.uppercaseChar() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmBlue.copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                // Student Selection
                if (recipientType == "student") {
                    Text(text = "Select Student:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    students.forEach { student ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStudentId = student.id }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStudentId == student.id,
                                onClick = { selectedStudentId = student.id }
                            )
                            Text(student.name ?: "Student ${student.id}")
                        }
                    }
                }

                // Cohort Selection (preset dropdown = fewer clicks; still allow custom)
                if (recipientType == "cohort") {
                    Text(text = "Cohort:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    val presets = listOf(
                        "grade_9" to "Grade 9",
                        "grade_10" to "Grade 10",
                        "grade_11" to "Grade 11",
                        "grade_12" to "Grade 12",
                        "all_students" to "All students"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presets.forEach { (value, label) ->
                            FilterChip(
                                selected = recipientCohort == value,
                                onClick = { recipientCohort = value },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CalmBlue.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = recipientCohort,
                        onValueChange = { recipientCohort = it },
                        label = { Text("Or type: grade_9, school_1, all_students") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Subject
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Message
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )

                // Priority
                Text(text = "Priority:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "normal", "high", "urgent").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.replaceFirstChar { it.uppercaseChar() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmBlue.copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                // Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = parentVisible,
                            onCheckedChange = { parentVisible = it }
                        )
                        Text("Visible to parents", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = emergencyOverride,
                            onCheckedChange = { emergencyOverride = it }
                        )
                        Text("Emergency override", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (showSuccess) {
                    Text(
                        text = "Message sent successfully!",
                        color = CalmGreen,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        isLoading = true
                        scope.launch {
                            commRepo.sendMessage(
                                recipientType = recipientType,
                                recipientId = if (recipientType == "student" && selectedStudentId > 0) selectedStudentId else null,
                                recipientCohort = if (recipientType == "cohort") recipientCohort else null,
                                subject = subject.takeIf { it.isNotBlank() },
                                message = message,
                                priority = priority,
                                parentVisible = parentVisible,
                                emergencyOverride = emergencyOverride
                            )
                                .onSuccess {
                                    showSuccess = true
                                    isLoading = false
                                    kotlinx.coroutines.delay(1500)
                                    onSend()
                                    onDismiss()
                                }
                                .onFailure {
                                    isLoading = false
                                }
                        }
                    }
                },
                enabled = !isLoading && message.isNotBlank() && 
                    (recipientType != "student" || selectedStudentId > 0) &&
                    (recipientType != "cohort" || recipientCohort.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Send")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

suspend fun loadSentMessages(
    repo: CommunicationRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (Result<Pair<List<com.mindaigle.data.remote.dto.SentMessage>, Int>>) -> Unit
) {
    scope.launch {
        val result = repo.getSentMessages()
        onResult(result)
    }
}

