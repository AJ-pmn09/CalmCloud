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
import com.mindaigle.data.remote.dto.AssistanceRequest
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociateRequests(
    userName: String,
    refreshKey: Int = 0,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var requests by remember { mutableStateOf<List<AssistanceRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedRequest by remember { mutableStateOf<AssistanceRequest?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val assistanceRepo = remember { AssistanceRepository() }

    // Refresh when refreshKey changes (tab navigation) or on initial load
    LaunchedEffect(refreshKey) {
        // Load data
        assistanceRepo.getRequests()
            .onSuccess { requestsList ->
                requests = requestsList
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
            assistanceRepo.getRequests()
                .onSuccess { requestsList ->
                    requests = requestsList
                }
        }
    }

    val pendingCount = requests.count { it.status == "pending" }
    val inProgressCount = requests.count { it.status == "in-progress" }
    val resolvedCount = requests.count { it.status == "resolved" }

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
                    Column {
                        Text(
                            text = "Chat & assistance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "When parents or students want to talk",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
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
                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RequestSummaryCard(
                        icon = Icons.Default.DateRange,
                        value = "$pendingCount",
                        label = "Pending",
                        color = StatusOkay,
                        modifier = Modifier.weight(1f)
                    )
                    RequestSummaryCard(
                        icon = Icons.Default.Info,
                        value = "$inProgressCount",
                        label = "In Progress",
                        color = CalmBlue,
                        modifier = Modifier.weight(1f)
                    )
                    RequestSummaryCard(
                        icon = Icons.Default.CheckCircle,
                        value = "$resolvedCount",
                        label = "Resolved",
                        color = CalmGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Chat / assistance requests (distinct from Alerts)
                Text(
                    text = "Assistance requests",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "Reply and chat with parents or students who requested support",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)
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
                } else if (requests.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF4E6)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "📭",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "No requests found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                text = "When parents create assistance requests, they'll appear here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    requests.forEach { request ->
                        AssistanceRequestCard(
                            request = request,
                            onTakeAction = {
                                selectedRequest = request
                                showActionDialog = true
                            },
                            onMarkResolved = {
                                scope.launch {
                                    assistanceRepo.updateRequest(request.id, "resolved", null)
                                        .onSuccess {
                                            assistanceRepo.getRequests()
                                                .onSuccess { requests = it }
                                        }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // Action Dialog
    selectedRequest?.let { request ->
        if (showActionDialog) {
            RequestActionDialog(
                request = request,
                onDismiss = {
                    showActionDialog = false
                    selectedRequest = null
                },
                onUpdate = { status, notes ->
                    scope.launch {
                        assistanceRepo.updateRequest(request.id, status, notes)
                            .onSuccess {
                                assistanceRepo.getRequests()
                                    .onSuccess { requests = it }
                                showActionDialog = false
                                selectedRequest = null
                            }
                    }
                }
            )
        }
    }
}

@Composable
fun RequestSummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
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
                        .clip(androidx.compose.foundation.shape.CircleShape)
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
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AssistanceRequestCard(
    request: AssistanceRequest,
    onTakeAction: () -> Unit,
    onMarkResolved: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with name and badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (request.studentNames != null && request.studentNames.size > 1) {
                            "${request.studentNames.size} Children"
                        } else {
                            request.studentName ?: request.studentNames?.firstOrNull() ?: "Unknown Student"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (request.studentNames != null && request.studentNames.size > 1) {
                        Text(
                            text = request.studentNames.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Urgency badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (request.urgency) {
                            "high" -> StatusUrgent.copy(alpha = 0.2f)
                            "normal" -> StatusOkay.copy(alpha = 0.2f)
                            else -> CalmGreen.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = request.urgency,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = when (request.urgency) {
                                "high" -> StatusUrgent
                                "normal" -> StatusOkay
                                else -> CalmGreen
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (request.status) {
                            "pending" -> StatusOkay.copy(alpha = 0.2f)
                            "in-progress" -> CalmBlue.copy(alpha = 0.2f)
                            else -> CalmGreen.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = request.status.replace("-", " "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = when (request.status) {
                                "pending" -> StatusOkay
                                "in-progress" -> CalmBlue
                                else -> CalmGreen
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // From parent and date
            Text(
                text = "From ${request.parentName} • ${formatDate(request.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Request message
            Text(
                text = request.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            // Response section if in-progress
            if (request.status == "in-progress" && !request.notes.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Your response:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = request.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (request.status == "pending") {
                    Button(
                        onClick = onTakeAction,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalmBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Take Action")
                    }
                }
                Button(
                    onClick = onMarkResolved,
                    modifier = Modifier.weight(1f),
                    enabled = request.status != "resolved",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalmGreen.copy(alpha = 0.2f),
                        contentColor = CalmGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mark Resolved")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestActionDialog(
    request: AssistanceRequest,
    onDismiss: () -> Unit,
    onUpdate: (String, String) -> Unit
) {
    var status by remember { mutableStateOf(request.status) }
    var notes by remember { mutableStateOf(request.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Take Action",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (request.studentNames != null && request.studentNames.size > 1) {
                        "Request from ${request.parentName} for ${request.studentNames.size} children"
                    } else {
                        "Request from ${request.parentName} for ${request.studentName ?: request.studentNames?.firstOrNull() ?: "student"}"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Status:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("pending", "in-progress", "resolved").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.replace("-", " ").replaceFirstChar { it.uppercaseChar() }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Response Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(status, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

fun formatDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: ""
    } catch (e: Exception) {
        dateString
    }
}

