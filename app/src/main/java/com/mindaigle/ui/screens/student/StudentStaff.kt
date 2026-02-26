package com.mindaigle.ui.screens.student

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mindaigle.data.repository.AppointmentRepository
import com.mindaigle.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentStaff(
    userName: String,
    onBack: (() -> Unit)? = null,
    onOpenMessages: () -> Unit = {}
) {
    var availableStaff by remember { mutableStateOf<List<com.mindaigle.data.remote.dto.StaffMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val appointmentRepo = remember { AppointmentRepository() }
    
    LaunchedEffect(Unit) {
        appointmentRepo.getStaffAvailability()
            .onSuccess {
                availableStaff = it
                isLoading = false
            }
            .onFailure {
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
            if (onBack != null) {
                TopAppBar(
                    title = { Text("Support Team", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = TextPrimary
                    )
                )
            }
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (onBack != null) 140.dp else 200.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4ECDC4),
                                Color(0xFF6EDDD6),
                                Color(0xFF95E1D3)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💚",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "Your Support Team",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${availableStaff.size} caring staff members ready to help",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = com.mindaigle.ui.theme.CalmBlue,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else if (availableStaff.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "👥",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "No staff available",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(availableStaff) { staff ->
                        StaffMemberCard(staff = staff, onStartChat = onOpenMessages)
                    }
                }
            }
        }
    }
}

@Composable
fun StaffMemberCard(
    staff: com.mindaigle.data.remote.dto.StaffMember,
    onStartChat: () -> Unit = {}
) {
    // Role-based gradient colors (iOS-style)
    val gradientColors = when (staff.role.lowercase()) {
        "admin" -> listOf(
            Color(0xFF6C5CE7), // Purple
            Color(0xFF8B7EE8)
        )
        "expert" -> listOf(
            Color(0xFF4ECDC4), // Turquoise
            Color(0xFF6EDDD6)
        )
        "associate" -> listOf(
            Color(0xFF95E1D3), // Mint
            Color(0xFFB4F0E2)
        )
        "staff" -> listOf(
            Color(0xFF74B9FF), // Sky Blue
            Color(0xFF95C9FF)
        )
        else -> listOf(
            Color(0xFFA8E6CF), // Soft Green
            Color(0xFFC8F5DD)
        )
    }
    
    // Role emoji
    val roleEmoji = when (staff.role.lowercase()) {
        "admin" -> "👑"
        "expert" -> "⭐"
        "associate" -> "💙"
        "staff" -> "🤝"
        else -> "👤"
    }
    
    Card(
        onClick = onStartChat,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 0f)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with emoji
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = roleEmoji,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                
                // Staff info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = staff.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "${roleEmoji} ${staff.role.replaceFirstChar { it.uppercaseChar() }}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                // Action button
                IconButton(
                    onClick = onStartChat,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        Icons.Default.Email,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
