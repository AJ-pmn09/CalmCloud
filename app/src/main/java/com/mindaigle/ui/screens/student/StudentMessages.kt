package com.mindaigle.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mindaigle.data.repository.CommunicationRepository
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMessages(
    userName: String,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var messages by remember { mutableStateOf<List<com.mindaigle.data.remote.dto.Communication>>(emptyList()) }
    var counselorNotes by remember { mutableStateOf<List<com.mindaigle.data.remote.dto.CounselorNote>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Messages, 1 = Notes
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var unreadCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val commRepo = remember { CommunicationRepository() }
    val noteRepo = remember { com.mindaigle.data.repository.CounselorNoteRepository() }

    fun loadMessages() {
        isLoading = true
        errorMessage = null
        scope.launch {
            commRepo.getMyMessages()
                .onSuccess { 
                    messages = it
                    unreadCount = it.count { it.status == "sent" }
                    isLoading = false
                }
                .onFailure { 
                    errorMessage = it.message
                    isLoading = false
                }
        }
    }

    fun loadCounselorNotes() {
        scope.launch {
            noteRepo.getCounselorNotes()
                .onSuccess {
                    counselorNotes = it
                }
                .onFailure {
                    // Silently fail - notes are optional
                }
        }
    }

    LaunchedEffect(Unit) {
        loadMessages()
        loadCounselorNotes()
        // Auto-refresh every 30 seconds
        while (true) {
            kotlinx.coroutines.delay(30000)
            loadMessages()
            loadCounselorNotes()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Messages" else "Counselor Notes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (selectedTab == 0 && unreadCount > 0) {
                            Badge(containerColor = CalmBlue) {
                                Text("$unreadCount", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedTab == 0) loadMessages() else loadCounselorNotes()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CalmBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )

            // Tab Selector - polished chips
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Email, null, Modifier.size(18.dp))
                                Text("Messages")
                                if (unreadCount > 0) {
                                    Badge(containerColor = Color.White) {
                                        Text("$unreadCount", style = MaterialTheme.typography.labelSmall, color = CalmBlue)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalmBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceLight,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Person, null, Modifier.size(18.dp))
                                Text("Notes")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalmBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceLight,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Card - gradient and icon
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(CalmBlue, CalmBlueDark)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Unread",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "$unreadCount",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Email, null, Modifier.size(28.dp), tint = Color.White)
                            }
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        if (isLoading && messages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CalmBlue)
                            }
                        } else if (errorMessage != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusUrgent.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.Info, null, Modifier.size(24.dp), tint = StatusUrgent)
                                    Text(
                                        text = errorMessage ?: "Error loading messages",
                                        color = StatusUrgent,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else if (messages.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(CalmBlue.copy(alpha = 0.06f), SurfaceLight)
                                            )
                                        )
                                        .padding(40.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(CalmBlue.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Email,
                                                null,
                                                modifier = Modifier.size(36.dp),
                                                tint = CalmBlue
                                            )
                                        }
                                        Text(
                                            text = "No messages yet",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "When your care team sends you a message, it will show up here.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(messages) { message ->
                                    StudentMessageCard(
                                        message = message,
                                        onRead = {
                                            scope.launch {
                                                commRepo.markMessageAsRead(message.id)
                                                    .onSuccess { loadMessages() }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (counselorNotes.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(CalmPurple.copy(alpha = 0.06f), SurfaceLight)
                                            )
                                        )
                                        .padding(40.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(CalmPurple.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                null,
                                                modifier = Modifier.size(36.dp),
                                                tint = CalmPurple
                                            )
                                        }
                                        Text(
                                            text = "No notes yet",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Counselor notes will appear here after your sessions.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(counselorNotes) { note ->
                                    CounselorNoteCard(note = note)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMessageDate(isoDate: String): String {
    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    )
    var parsed: Date? = null
    for (fmt in formats) {
        try {
            parsed = fmt.parse(isoDate)
            if (parsed != null) break
        } catch (_: Exception) {}
    }
    if (parsed == null) return isoDate
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = parsed }
    val today = now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) && now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yesterday = yesterdayCal.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) && yesterdayCal.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    return when {
        today -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed)
        yesterday -> "Yesterday, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed)
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(parsed)
    }
}

@Composable
fun StudentMessageCard(
    message: com.mindaigle.data.remote.dto.Communication,
    onRead: () -> Unit
) {
    val dateStr = formatMessageDate(message.createdAt)
    val isUnread = message.status == "sent"
    val priorityColor = when (message.priority.lowercase()) {
        "urgent" -> StatusUrgent
        "high" -> StatusSupport
        "normal" -> CalmBlue
        "low" -> CalmGreen
        else -> TextSecondary
    }
    val senderInitial = message.senderName.take(1).uppercase()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isUnread) onRead() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnread) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) priorityColor.copy(alpha = 0.06f) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(CalmBlue, CalmBlueDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = senderInitial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (isUnread) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = priorityColor
                            ) {
                                Text(
                                    text = "New",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "${message.senderRole.replaceFirstChar { it.uppercaseChar() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (!message.subject.isNullOrBlank()) {
                    Text(
                        text = message.subject ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUnread) TextPrimary else TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PriorityBadge(message.priority)
                    if (message.emergencyOverride) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusUrgent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Emergency",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusUrgent,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
fun CounselorNoteCard(note: com.mindaigle.data.remote.dto.CounselorNote) {
    val dateStr = formatMessageDate(note.createdAt)
    val name = note.counselorName ?: "Counselor"
    val initial = name.take(1).uppercase()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CalmPurple, CalmPurpleLight))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CalmPurple.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Note",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = CalmPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = "${note.counselorRole?.replaceFirstChar { it.uppercaseChar() } ?: "Staff"} · $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                HorizontalDivider(color = SurfaceLight)
                Text(
                    text = note.noteText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}
