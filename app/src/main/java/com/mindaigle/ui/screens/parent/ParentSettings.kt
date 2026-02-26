package com.mindaigle.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.mindaigle.ui.theme.*
import com.mindaigle.data.repository.AuthRepository
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettings(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf<com.mindaigle.data.remote.dto.User?>(null) }
    val context = LocalContext.current
    val authRepo = remember { AuthRepository() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                authRepo.uploadProfilePhoto(base64)
                    .onSuccess {
                        authRepo.getProfile().onSuccess { userProfile = it }
                        snackbarHostState.showSnackbar("Profile photo updated")
                    }
                    .onFailure { snackbarHostState.showSnackbar("Failed to upload: ${it.message}") }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!com.mindaigle.data.remote.AuthManager.isTestMode()) {
            authRepo.getProfile().onSuccess { userProfile = it }
        }
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
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateToTab(0) }) {
                        Icon(Icons.Default.Menu, contentDescription = "Back to Home")
                    }
                },
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) {
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
                // Profile Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(CalmBlue, CalmBlueDark)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // Account Settings
                SettingsSection(title = "Account") {
                    SettingsMenuItem(
                        icon = Icons.Default.Person,
                        title = "Profile Information",
                        subtitle = "Update your personal details",
                        onClick = { showProfileDialog = true }
                    )
                    SettingsMenuItem(
                        icon = Icons.Default.Lock,
                        title = "Privacy & Security",
                        subtitle = "Manage your account security"
                    )
                    SettingsMenuItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Configure notification preferences"
                    )
                }

                // Child Management
                SettingsSection(title = "Child Management") {
                    SettingsMenuItem(
                        icon = Icons.Default.Person,
                        title = "Linked Children",
                        subtitle = "View and manage linked accounts"
                    )
                    SettingsMenuItem(
                        icon = Icons.Default.Info,
                        title = "Parent-Child Linking",
                        subtitle = "Learn about account linking"
                    )
                }

                // App Settings
                SettingsSection(title = "App Settings") {
                    SettingsMenuItem(
                        icon = Icons.Default.Settings,
                        title = "Preferences",
                        subtitle = "Customize your experience"
                    )
                    SettingsMenuItem(
                        icon = Icons.Default.Info,
                        title = "Help & Support",
                        subtitle = "Get help and contact support"
                    )
                    SettingsMenuItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "App version and information"
                    )
                }

                // Logout Button
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusUrgent
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Profile Settings Dialog
    if (showProfileDialog) {
        ProfileSettingsDialog(
            userName = userName,
            userEmail = userEmail,
            userRole = "parent",
            profileImageUrl = userProfile?.profilePictureUrl,
            onDismiss = { showProfileDialog = false },
            onLogout = onLogout,
            onRequestChangePhoto = if (com.mindaigle.data.remote.AuthManager.isTestMode()) null else { { photoPickerLauncher.launch("image/*") } }
        )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CalmBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = CalmBlue, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

