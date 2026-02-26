package com.mindaigle.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import com.mindaigle.ui.theme.*
import com.mindaigle.ui.components.ProfilePicture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsDialog(
    userName: String,
    userEmail: String,
    userRole: String,
    profileImageUrl: String? = null,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onUpdateProfile: (name: String, email: String) -> Unit = { _, _ -> },
    onRequestChangePhoto: (() -> Unit)? = null
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(userName) }
    var editedEmail by remember { mutableStateOf(userEmail) }
    var isEditing by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUrgent)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, null)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile Header with photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (onRequestChangePhoto != null) 160.dp else 140.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(CalmBlue, CalmPurple)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            ProfilePicture(
                                imageUrl = profileImageUrl,
                                userName = userName,
                                size = 72.dp,
                                backgroundColor = Color.White.copy(alpha = 0.3f),
                                textColor = Color.White
                            )
                            if (onRequestChangePhoto != null) {
                                IconButton(onClick = { onRequestChangePhoto.invoke() }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Change photo",
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userRole.replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                // Profile Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = editedEmail,
                                onValueChange = { editedEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                shape = RoundedCornerShape(12.dp),
                                enabled = false // Email usually can't be changed
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isEditing = false
                                        editedName = userName
                                        editedEmail = userEmail
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        onUpdateProfile(editedName, editedEmail)
                                        isEditing = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                                ) {
                                    Text("Save")
                                }
                            }
                        } else {
                            ProfileInfoRow(
                                icon = Icons.Default.Person,
                                label = "Name",
                                value = userName
                            )
                            ProfileInfoRow(
                                icon = Icons.Default.Info,
                                label = "Email",
                                value = userEmail
                            )
                            ProfileInfoRow(
                                icon = Icons.Default.Info,
                                label = "Role",
                                value = userRole.replaceFirstChar { it.uppercaseChar() }
                            )
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                            ) {
                                Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Profile")
                            }
                        }
                    }
                }

                // App Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "App Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        ProfileInfoRow(
                            icon = Icons.Default.Info,
                            label = "Version",
                            value = "1.0.0"
                        )
                        ProfileInfoRow(
                            icon = Icons.Default.Star,
                            label = "Data Format",
                            value = "FHIR R4"
                        )
                    }
                }

                // Logout Button
                Button(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUrgent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            }
        },
    )
}

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = CalmBlue, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

