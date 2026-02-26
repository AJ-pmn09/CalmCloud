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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindaigle.data.remote.dto.EmergencyAlert
import com.mindaigle.data.remote.dto.SuicideRiskScreeningResponse
import com.mindaigle.data.repository.AlertRepository
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociateAlerts(
    userName: String,
    refreshKey: Int = 0,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var alerts by remember { mutableStateOf<List<EmergencyAlert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedAlert by remember { mutableStateOf<EmergencyAlert?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showScreeningDialog by remember { mutableStateOf(false) }
    var screeningData by remember { mutableStateOf<SuicideRiskScreeningResponse?>(null) }
    var screeningLoading by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf("") } // "acknowledge" or "resolve"
    var resolutionNotes by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val alertRepo = remember { AlertRepository() }
    val scrollState = rememberScrollState()

    // Load alerts function
    fun loadAlerts() {
        scope.launch {
            alertRepo.getEmergencyAlerts()
                .onSuccess { alertsList ->
                    alerts = alertsList
                    isLoading = false
                }
                .onFailure {
                    isLoading = false
                }
        }
    }

    // Refresh when refreshKey changes (tab navigation) or on initial load
    LaunchedEffect(refreshKey) {
        loadAlerts()
    }
    
    // Automatic periodic refresh every 10 seconds for alerts (more frequent for critical data)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000) // 10 seconds
            loadAlerts()
        }
    }

    val activeAlerts = alerts.filter { it.status == "active" }
    val acknowledgedAlerts = alerts.filter { it.status == "acknowledged" }
    val resolvedAlerts = alerts.filter { it.status == "resolved" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = CalmBlue
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header - Alerts = crisis & urgent (distinct from Chat/Requests)
                Text(
                    text = "Emergency Alerts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Crisis & urgent support — acknowledge and resolve",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Alert Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AlertSummaryCard(
                        title = "Active",
                        count = activeAlerts.size,
                        color = StatusUrgent,
                        modifier = Modifier.weight(1f)
                    )
                    AlertSummaryCard(
                        title = "Acknowledged",
                        count = acknowledgedAlerts.size,
                        color = StatusOkay,
                        modifier = Modifier.weight(1f)
                    )
                    AlertSummaryCard(
                        title = "Resolved",
                        count = resolvedAlerts.size,
                        color = CalmGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Active Alerts Section
                if (activeAlerts.isNotEmpty()) {
                    Text(
                        text = "Active Alerts (${activeAlerts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    activeAlerts.forEach { alert ->
                        AlertCard(
                            alert = alert,
                            onClick = {
                                selectedAlert = alert
                                actionType = "acknowledge"
                                showActionDialog = true
                            }
                        )
                    }
                }

                // Acknowledged Alerts Section
                if (acknowledgedAlerts.isNotEmpty()) {
                    Text(
                        text = "Acknowledged Alerts (${acknowledgedAlerts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    acknowledgedAlerts.forEach { alert ->
                        AlertCard(
                            alert = alert,
                            onClick = {
                                selectedAlert = alert
                                actionType = "resolve"
                                showActionDialog = true
                            }
                        )
                    }
                }

                // Resolved Alerts Section
                if (resolvedAlerts.isNotEmpty()) {
                    Text(
                        text = "Resolved Alerts (${resolvedAlerts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    resolvedAlerts.forEach { alert ->
                        AlertCard(
                            alert = alert,
                            onClick = null
                        )
                    }
                }

                // Empty State
                if (alerts.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = CalmGreen
                            )
                            Text(
                                text = "No Alerts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "All clear! No emergency alerts at this time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // Action Dialog (Acknowledge/Resolve)
    if (showActionDialog && selectedAlert != null) {
        AlertDialog(
            onDismissRequest = {
                showActionDialog = false
                resolutionNotes = ""
            },
            title = {
                Text(
                    text = if (actionType == "acknowledge") "Acknowledge Alert" else "Resolve Alert",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    selectedAlert?.let { alert ->
                        Text(text = "Student: ${alert.studentName ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Type: ${alert.alertType.replaceFirstChar { it.uppercaseChar() }}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Message: ${alert.message ?: "No message provided"}", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = {
                                screeningLoading = true
                                scope.launch {
                                    alertRepo.getSuicideRiskScreening(alert.id)
                                        .onSuccess { screening ->
                                            screeningData = screening
                                            showActionDialog = false
                                            showScreeningDialog = true
                                        }
                                        .onFailure { screeningData = null }
                                    screeningLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (screeningLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CalmBlue)
                            } else {
                                Text("View safety screening")
                            }
                        }
                        if (actionType == "resolve") {
                            OutlinedTextField(
                                value = resolutionNotes,
                                onValueChange = { resolutionNotes = it },
                                label = { Text("Resolution Notes (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedAlert?.let { alert ->
                            isProcessing = true
                            scope.launch {
                                val result = if (actionType == "acknowledge") {
                                    alertRepo.acknowledgeAlert(alert.id)
                                } else {
                                    alertRepo.resolveAlert(alert.id, resolutionNotes.takeIf { it.isNotBlank() })
                                }
                                
                                result.onSuccess {
                                    loadAlerts() // Refresh alerts
                                    showActionDialog = false
                                    resolutionNotes = ""
                                    selectedAlert = null
                                }
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "acknowledge") StatusOkay else CalmGreen
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(if (actionType == "acknowledge") "Acknowledge" else "Resolve")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showActionDialog = false
                    resolutionNotes = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Screening result dialog (staff view of safety screening)
    if (showScreeningDialog && screeningData != null) {
        val screening = screeningData!!
        val riskColor = when (screening.riskLevel) {
            "critical" -> StatusUrgent
            "high" -> StatusSupport
            "moderate" -> StatusOkay
            else -> CalmGreen
        }
        AlertDialog(
            onDismissRequest = {
                showScreeningDialog = false
                screeningData = null
                selectedAlert = null
            },
            title = {
                Text(
                    text = "Safety screening",
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Student: ${screening.studentName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Risk score:", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${screening.riskScore}/10", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = riskColor)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Risk level:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = screening.riskLevel.replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = riskColor
                        )
                    }
                    if (screening.immediateActionRequired) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusUrgent.copy(alpha = 0.15f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Immediate action recommended",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusUrgent
                                )
                                Text(
                                    text = "Call 988 (Suicide & Crisis Lifeline) or 911 if the student is in immediate danger.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = "Screening responses (Q&A):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    screening.screeningQuestions.entries.forEach { (q, v) ->
                        val ans = (v as? String) ?: v.toString()
                        Text(text = "• $q: $ans", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showScreeningDialog = false
                        screeningData = null
                        selectedAlert?.let { alert ->
                            selectedAlert = alert
                            showActionDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                ) {
                    Text("Back to alert")
                }
            }
        )
    }
}

@Composable
fun AlertSummaryCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AlertCard(
    alert: EmergencyAlert,
    onClick: (() -> Unit)?
) {
    val alertColor = when (alert.alertType) {
        "emergency" -> StatusUrgent
        "urgent" -> StatusSupport
        "support" -> CalmBlue
        else -> Color.Gray
    }

    val statusColor = when (alert.status) {
        "active" -> StatusUrgent
        "acknowledged" -> StatusOkay
        "resolved" -> CalmGreen
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = alertColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = alert.alertType.replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = alertColor
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = alert.status.replaceFirstChar { it.uppercaseChar() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (alert.studentName != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = alert.studentName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (alert.grade != null) {
                        Text(
                            text = "Grade ${alert.grade}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (alert.message != null && alert.message.isNotBlank()) {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(alert.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                if (onClick != null && alert.status == "active") {
                    TextButton(onClick = onClick) {
                        Text("Take Action", color = CalmBlue)
                    }
                } else if (alert.status == "acknowledged") {
                    TextButton(onClick = onClick ?: {}) {
                        Text("Resolve", color = CalmGreen)
                    }
                }
            }

            if (alert.acknowledgedByName != null) {
                Text(
                    text = "Acknowledged by ${alert.acknowledgedByName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            if (alert.resolvedByName != null) {
                Text(
                    text = "Resolved by ${alert.resolvedByName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CalmGreen,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                if (alert.resolutionNotes != null && alert.resolutionNotes.isNotBlank()) {
                    Text(
                        text = "Notes: ${alert.resolutionNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

