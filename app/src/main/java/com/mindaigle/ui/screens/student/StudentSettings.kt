package com.mindaigle.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mindaigle.ui.components.ProfileSettingsDialog
import com.mindaigle.ui.components.ProfilePicture
import com.mindaigle.ui.components.ProfilePictureShape
import com.mindaigle.ui.theme.*
import com.mindaigle.data.repository.ReminderRepository
import com.mindaigle.data.repository.AuthRepository
import com.mindaigle.workers.ReminderScheduler
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import com.mindaigle.data.repository.AlertRepository
import com.mindaigle.data.repository.StudentRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSettings(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    onOpenWearable: () -> Unit = {},
    onNavigateToTab: (Int) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showReminderSettings by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var studentInfo by remember { mutableStateOf<com.mindaigle.data.remote.dto.StudentInfo?>(null) }
    var userProfile by remember { mutableStateOf<com.mindaigle.data.remote.dto.User?>(null) }
    val repository = remember { StudentRepository() }
    val authRepo = remember { AuthRepository() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                authRepo.uploadProfilePhoto(base64)
                    .onSuccess {
                        authRepo.getProfile().onSuccess { userProfile = it }
                        repository.getMyStudentData().onSuccess { r -> studentInfo = r.student }
                        snackbarHostState.showSnackbar("Profile photo updated")
                    }
                    .onFailure { snackbarHostState.showSnackbar("Failed to upload: ${it.message}") }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        repository.getMyStudentData()
            .onSuccess { response -> studentInfo = response.student }
        if (!com.mindaigle.data.remote.AuthManager.isTestMode()) {
            authRepo.getProfile().onSuccess { userProfile = it }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateToTab(0) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home")
                    }
                },
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) {
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
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile Header with Gradient
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(CalmBlue, CalmBlueDark)
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfilePicture(
                                imageUrl = studentInfo?.profilePictureUrl,
                                userName = studentInfo?.name ?: studentInfo?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() } ?: userName,
                                size = 64.dp,
                                shape = ProfilePictureShape.Rounded,
                                cornerRadius = 16.dp,
                                backgroundColor = Color.Transparent,
                                textColor = Color.White
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                                            text = "Student Account",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Settings Options
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = "Profile Settings",
                            description = "Update your personal information",
                            onClick = { showProfileDialog = true }
                        )
                        HorizontalDivider()
                        // Emergency Alert Button
                        EmergencyAlertButton()
                        HorizontalDivider()
                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = "Check-in Reminders",
                            description = "Configure missed check-in reminders",
                            onClick = { showReminderSettings = true }
                        )
                        HorizontalDivider()
                        SettingsItem(
                            icon = Icons.Default.FitnessCenter,
                            title = "Connect Health Data",
                            description = "Grant read access and see steps, sleep, and activity on the Wearable tab",
                            onClick = { onOpenWearable() }
                        )
                        HorizontalDivider()
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Privacy & Data",
                            description = "Control your FHIR data sharing",
                            onClick = { showPrivacyDialog = true }
                        )
                        HorizontalDivider()
                        SettingsItem(
                            icon = Icons.Default.Favorite,
                            title = "Wellness Resources",
                            description = "Access mental health resources",
                            onClick = { onNavigateToTab(5) }
                        )
                        HorizontalDivider()
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Help & Support",
                            description = "Get help with the app",
                            onClick = { showHelpDialog = true }
                        )
                    }
                }

                // About Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        InfoRow("Version", "1.0.0 (Mobile Beta)")
                        InfoRow("Data Format", "FHIR R4")
                        InfoRow("Last Sync", "Just now")
                    }
                }

                // Sign Out Button
                TextButton(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = StatusUrgent
                    )
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Profile Dialog
    if (showProfileDialog) {
        ProfileSettingsDialog(
            userName = userName,
            userEmail = userEmail,
            userRole = "student",
            profileImageUrl = userProfile?.profilePictureUrl ?: studentInfo?.profilePictureUrl,
            onDismiss = { showProfileDialog = false },
            onLogout = onLogout,
            onRequestChangePhoto = if (com.mindaigle.data.remote.AuthManager.isTestMode()) null else {{ photoPickerLauncher.launch("image/*") }}
        )
    }

    // Reminder Settings Dialog
    if (showReminderSettings) {
        ReminderSettingsDialog(
            onDismiss = { showReminderSettings = false }
        )
    }

    // Logout Confirmation
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUrgent)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Data", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Your wellness data is stored securely and used only to provide you and your care team with insights. You can control FHIR data sharing in your school's portal. This app does not sell your data.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK", color = CalmBlue)
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Help & Support", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "For app support, contact your school counselor or the MindAIgle support team. Use the Resources tab for community and crisis resources. In an emergency, call 988 or your local crisis line.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("OK", color = CalmBlue)
                }
            }
        )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ReminderSettingsDialog(onDismiss: () -> Unit) {
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderIntervalHours by remember { mutableIntStateOf(24) }
    var smartScheduling by remember { mutableStateOf(true) }
    var quietHoursStart by remember { mutableStateOf("22:00") }
    var quietHoursEnd by remember { mutableStateOf("07:00") }
    var maxPerDay by remember { mutableIntStateOf(3) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val reminderRepo = remember { ReminderRepository() }

    LaunchedEffect(Unit) {
        reminderRepo.getReminderConfig()
            .onSuccess { config ->
                reminderEnabled = config.reminderEnabled
                reminderIntervalHours = config.reminderIntervalHours
                smartScheduling = config.smartScheduling ?: true
                quietHoursStart = config.quietHoursStart?.substringBefore(":")?.let { 
                    val parts = config.quietHoursStart.split(":")
                    "${parts[0]}:${parts[1]}"
                } ?: "22:00"
                quietHoursEnd = config.quietHoursEnd?.substringBefore(":")?.let {
                    val parts = config.quietHoursEnd.split(":")
                    "${parts[0]}:${parts[1]}"
                } ?: "07:00"
                maxPerDay = config.maxPerDay ?: 3
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Check-in Reminder Settings",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Enable Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Get notified when you miss a check-in",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                }

                if (reminderEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Reminder Interval
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Reminder Interval",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "$reminderIntervalHours hours",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CalmBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = reminderIntervalHours.toFloat(),
                                onValueChange = { reminderIntervalHours = it.toInt() },
                                valueRange = 1f..168f,
                                steps = 166,
                                colors = SliderDefaults.colors(
                                    thumbColor = CalmBlue,
                                    activeTrackColor = CalmBlue
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1 hour", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("1 week", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        
                        HorizontalDivider()
                        
                        // Smart Scheduling
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Smart Scheduling",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Avoid notification fatigue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = smartScheduling,
                                onCheckedChange = { smartScheduling = it }
                            )
                        }
                        
                        if (smartScheduling) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Max reminders per day
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Max Reminders Per Day",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "$maxPerDay",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CalmBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = maxPerDay.toFloat(),
                                        onValueChange = { maxPerDay = it.toInt() },
                                        valueRange = 1f..10f,
                                        steps = 8,
                                        colors = SliderDefaults.colors(
                                            thumbColor = CalmBlue,
                                            activeTrackColor = CalmBlue
                                        )
                                    )
                                }
                                
                                // Quiet Hours
                                Text(
                                    "Quiet Hours (No reminders)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = quietHoursStart,
                                        onValueChange = { quietHoursStart = it },
                                        label = { Text("Start") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = quietHoursEnd,
                                        onValueChange = { quietHoursEnd = it },
                                        label = { Text("End") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSuccess) {
                    Text(
                        "Settings saved successfully!",
                        color = CalmGreen,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        reminderRepo.updateReminderConfig(
                            reminderEnabled = reminderEnabled,
                            reminderIntervalHours = reminderIntervalHours,
                            smartScheduling = if (reminderEnabled) smartScheduling else null,
                            quietHoursStart = if (reminderEnabled && smartScheduling) "$quietHoursStart:00" else null,
                            quietHoursEnd = if (reminderEnabled && smartScheduling) "$quietHoursEnd:00" else null,
                            maxPerDay = if (reminderEnabled && smartScheduling) maxPerDay else null
                        )
                            .onSuccess {
                                val qStart = if (reminderEnabled && smartScheduling) "$quietHoursStart:00" else null
                                val qEnd = if (reminderEnabled && smartScheduling) "$quietHoursEnd:00" else null
                                ReminderScheduler.schedule(context, reminderEnabled, reminderIntervalHours, qStart, qEnd)
                                showSuccess = true
                                isLoading = false
                                kotlinx.coroutines.delay(1500)
                                onDismiss()
                            }
                            .onFailure {
                                isLoading = false
                            }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Save")
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

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CalmBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = CalmBlue, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Icon(Icons.Default.Info, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmergencyAlertButton() {
    var showAlertDialog by remember { mutableStateOf(false) }
    var showScreeningDialog by remember { mutableStateOf(false) }
    var alertType by remember { mutableStateOf("emergency") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val alertRepo = remember { AlertRepository() }
    
    // Suicide-risk screening questions
    val screeningQuestions = remember {
        listOf(
            "Have you had thoughts about hurting yourself?" to mutableStateOf(""),
            "Have you made a plan to hurt yourself?" to mutableStateOf(""),
            "Do you feel hopeless about the future?" to mutableStateOf(""),
            "Have you felt like you want to die?" to mutableStateOf("")
        )
    }

    Box {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StatusUrgent.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Button(
                onClick = { showScreeningDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(StatusUrgent, StatusUrgent.copy(alpha = 0.8f))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Emergency Alert - Contact MH Support",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        // Snackbar host for success/error messages (overlays the card)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showScreeningDialog) {
        AlertDialog(
            onDismissRequest = { showScreeningDialog = false },
            title = {
                Text(
                    "Safety Check",
                    fontWeight = FontWeight.Bold,
                    color = StatusUrgent
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Before we proceed, please answer these safety questions:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    screeningQuestions.forEach { (question, responseState) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(question, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("No", "Sometimes", "Yes").forEach { option ->
                                    FilterChip(
                                        selected = responseState.value == option.lowercase(),
                                        onClick = { responseState.value = option.lowercase() },
                                        label = { Text(option) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (option == "Yes") StatusUrgent.copy(alpha = 0.2f) else CalmBlue.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "If you're in immediate danger, please call 988 (Suicide & Crisis Lifeline) or 911.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusUrgent
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Create screening object
                        val screening = com.mindaigle.data.remote.dto.SuicideRiskScreening(
                            questions = screeningQuestions.map { (q, r) ->
                                com.mindaigle.data.remote.dto.SuicideRiskScreeningQuestion(q, r.value)
                            }
                        )
                        
                        // Close screening dialog and show alert dialog
                        showScreeningDialog = false
                        showAlertDialog = true
                        
                        // Store screening for later use
                        // We'll pass it when sending the alert
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScreeningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            title = {
                Text(
                    "Emergency Alert",
                    fontWeight = FontWeight.Bold,
                    color = StatusUrgent
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select alert type:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("emergency", "urgent", "support").forEach { type ->
                            FilterChip(
                                selected = alertType == type,
                                onClick = { alertType = type },
                                label = { Text(type.replaceFirstChar { it.uppercaseChar() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatusUrgent.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message (optional)") },
                        placeholder = { Text("Describe your situation...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            // Create screening object from responses
                            val screening = com.mindaigle.data.remote.dto.SuicideRiskScreening(
                                questions = screeningQuestions.map { (q, r) ->
                                    com.mindaigle.data.remote.dto.SuicideRiskScreeningQuestion(q, r.value)
                                }
                            )
                            
                            alertRepo.createEmergencyAlert(
                                alertType, 
                                message.takeIf { it.isNotBlank() },
                                screening
                            )
                                .onSuccess {
                                    isLoading = false
                                    // Close dialog immediately
                                    showAlertDialog = false
                                    message = ""
                                    // Reset screening responses
                                    screeningQuestions.forEach { (_, r) -> r.value = "" }
                                    // Show success snackbar
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Alert sent successfully! Support staff will contact you soon.",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                                .onFailure { error ->
                                    isLoading = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to send alert: ${error.message}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUrgent)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Send Alert")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlertDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

