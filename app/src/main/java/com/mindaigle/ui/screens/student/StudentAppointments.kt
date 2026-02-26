package com.mindaigle.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindaigle.data.remote.dto.Appointment
import com.mindaigle.data.remote.dto.CreateAppointmentRequest
import com.mindaigle.data.remote.dto.StaffMember
import com.mindaigle.data.repository.AppointmentRepository
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAppointments(
    userName: String,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var staffMembers by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val appointmentRepo = remember { AppointmentRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadAppointments() {
        isLoading = true
        scope.launch {
            try {
                appointmentRepo.getAppointments()
                    .onSuccess {
                        appointments = it
                        isLoading = false
                    }
                    .onFailure {
                        isLoading = false
                        snackbarHostState.showSnackbar("Failed to load appointments: ${it.message}")
                    }
            } catch (e: Throwable) {
                android.util.Log.e("StudentAppointments", "getAppointments error", e)
                isLoading = false
                snackbarHostState.showSnackbar("Failed to load appointments")
            }
        }
    }

    fun loadStaff() {
        scope.launch {
            try {
                appointmentRepo.getStaffAvailability()
                    .onSuccess { staffMembers = it }
                    .onFailure {
                        snackbarHostState.showSnackbar("Failed to load staff: ${it.message}")
                    }
            } catch (e: Throwable) {
                android.util.Log.e("StudentAppointments", "getStaff error", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            loadAppointments()
            loadStaff()
            while (true) {
                kotlinx.coroutines.delay(30000)
                try {
                    loadAppointments()
                } catch (e: Throwable) {
                    android.util.Log.e("StudentAppointments", "Refresh failed", e)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("StudentAppointments", "Load failed", e)
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Appointments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { loadAppointments() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh appointments", tint = TextPrimary)
                    }
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp),
                        containerColor = CalmBlue
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Book appointment", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )

            if (isLoading && appointments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CalmBlue)
                }
            } else if (appointments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary
                        )
                        Text(
                            text = "No appointments yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Schedule a meeting with staff",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Book Appointment")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(appointments) { appointment ->
                        AppointmentCard(
                            appointment = appointment,
                            onClick = {
                                selectedAppointment = appointment
                                showDetailsDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Create Appointment Dialog
        if (showCreateDialog) {
            CreateAppointmentDialog(
                staffMembers = staffMembers,
                onDismiss = { showCreateDialog = false },
                onConfirm = { request ->
                    scope.launch {
                        appointmentRepo.createAppointment(request)
                            .onSuccess {
                                showCreateDialog = false
                                loadAppointments()
                                snackbarHostState.showSnackbar("Appointment created successfully!")
                            }
                            .onFailure {
                                snackbarHostState.showSnackbar("Failed to create appointment: ${it.message}")
                            }
                    }
                }
            )
        }

        // Appointment Details Dialog
        selectedAppointment?.let { appointment ->
            if (showDetailsDialog) {
                AppointmentDetailsDialog(
                    appointment = appointment,
                    onDismiss = {
                        showDetailsDialog = false
                        selectedAppointment = null
                    },
                    onCancel = {
                        scope.launch {
                            appointmentRepo.cancelAppointment(appointment.id)
                                .onSuccess {
                                    showDetailsDialog = false
                                    selectedAppointment = null
                                    loadAppointments()
                                    snackbarHostState.showSnackbar("Appointment cancelled")
                                }
                                .onFailure {
                                    snackbarHostState.showSnackbar("Failed to cancel: ${it.message}")
                                }
                        }
                    }
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        null,
                        tint = CalmBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = formatAppointmentDate(appointment.appointmentDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = formatAppointmentTime(appointment.appointmentDate, appointment.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                StatusChip(status = appointment.status)
            }

            if (appointment.staffName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = appointment.staffName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (appointment.staffRole != null) {
                        Text(
                            text = "• ${appointment.staffRole}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (appointment.notes != null && appointment.notes.isNotBlank()) {
                Text(
                    text = appointment.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (color, text) = when (status.lowercase()) {
        "pending" -> CalmBlue to "Pending"
        "confirmed" -> Color(0xFF4CAF50) to "Confirmed"
        "completed" -> Color(0xFF9E9E9E) to "Completed"
        "cancelled" -> Color(0xFFF44336) to "Cancelled"
        else -> TextSecondary to status
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CreateAppointmentDialog(
    staffMembers: List<StaffMember>,
    onDismiss: () -> Unit,
    onConfirm: (CreateAppointmentRequest) -> Unit
) {
    var selectedStaffId by remember { mutableStateOf<Int?>(null) }
    var appointmentDate by remember { mutableStateOf("") }
    var appointmentTime by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(30) }
    var type by remember { mutableStateOf("general") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book Appointment", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Staff Selection
                if (staffMembers.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = staffMembers.find { it.id == selectedStaffId }?.name ?: "Select Staff (Optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Staff Member") },
                            trailingIcon = {
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        null
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            staffMembers.forEach { staff ->
                                DropdownMenuItem(
                                    text = { Text("${staff.name} (${staff.role})") },
                                    onClick = {
                                        selectedStaffId = staff.id
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("No specific staff") },
                                onClick = {
                                    selectedStaffId = null
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Date
                OutlinedTextField(
                    value = appointmentDate,
                    onValueChange = { appointmentDate = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-01-25") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Time
                OutlinedTextField(
                    value = appointmentTime,
                    onValueChange = { appointmentTime = it },
                    label = { Text("Time (HH:MM)") },
                    placeholder = { Text("14:30") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Duration:", modifier = Modifier.align(Alignment.CenterVertically))
                    listOf(15, 30, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = duration == mins,
                            onClick = { duration = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                // Type
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type") },
                    placeholder = { Text("general, counseling, check-in") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Any additional information...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (appointmentDate.isNotBlank() && appointmentTime.isNotBlank()) {
                        val dateTime = "$appointmentDate $appointmentTime:00"
                        onConfirm(
                            CreateAppointmentRequest(
                                staffId = selectedStaffId,
                                appointmentDate = dateTime,
                                duration = duration,
                                type = type,
                                notes = notes.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = appointmentDate.isNotBlank() && appointmentTime.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
            ) {
                Text("Book")
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
fun AppointmentDetailsDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appointment Details", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow("Date", formatAppointmentDate(appointment.appointmentDate))
                DetailRow("Time", formatAppointmentTime(appointment.appointmentDate, appointment.duration))
                DetailRow("Duration", "${appointment.duration} minutes")
                DetailRow("Type", appointment.type)
                DetailRow("Status", appointment.status)
                if (appointment.staffName != null) {
                    DetailRow("Staff", appointment.staffName)
                }
                if (appointment.notes != null && appointment.notes.isNotBlank()) {
                    DetailRow("Notes", appointment.notes)
                }
            }
        },
        confirmButton = {
            if (appointment.status == "pending" || appointment.status == "confirmed") {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Cancel Appointment")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

fun formatAppointmentDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString) ?: return dateString
        outputFormat.format(date)
    } catch (e: Exception) {
        dateString
    }
}

fun formatAppointmentTime(dateString: String, duration: Int): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = inputFormat.parse(dateString) ?: return dateString
        val endTime = Calendar.getInstance().apply {
            time = date
            add(Calendar.MINUTE, duration)
        }.time
        "${timeFormat.format(date)} - ${timeFormat.format(endTime)}"
    } catch (e: Exception) {
        dateString
    }
}
