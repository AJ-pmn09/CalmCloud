package com.example.calmcloud.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.calmcloud.data.repository.AssistanceRepository
import com.example.calmcloud.data.repository.StudentRepository
import com.example.calmcloud.data.remote.dto.AssistanceRequest
import com.example.calmcloud.data.remote.dto.Child
import com.example.calmcloud.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentMessages(userName: String) {
    var children by remember { mutableStateOf<List<Child>>(emptyList()) }
    var requests by remember { mutableStateOf<List<AssistanceRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("normal") }
    var selectedChildId by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val studentRepo = remember { StudentRepository() }
    val assistanceRepo = remember { AssistanceRepository() }

    LaunchedEffect(Unit) {
        studentRepo.getChildren()
            .onSuccess { childrenList ->
                children = childrenList
                if (childrenList.isNotEmpty()) {
                    selectedChildId = childrenList.first().id
                }
            }
        assistanceRepo.getRequests()
            .onSuccess { requestsList ->
                requests = requestsList
                isLoading = false
            }
            .onFailure {
                isLoading = false
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
                        text = "Messages",
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
                // Send Message to Care Team Card
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
                            text = "Send Message to Care Team",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Message",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Describe your concern or question...") },
                            minLines = 4,
                            maxLines = 6,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF5F5F5),
                                unfocusedContainerColor = Color(0xFFF5F5F5)
                            )
                        )

                        Text(
                            text = "Priority",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("low", "normal", "high").forEach { level ->
                                FilterChip(
                                    selected = priority == level,
                                    onClick = { priority = level },
                                    label = {
                                        Text(
                                            level.replaceFirstChar { it.uppercaseChar() },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (level) {
                                            "high" -> StatusUrgent
                                            "normal" -> StatusOkay
                                            else -> Color(0xFFE5E5E5)
                                        },
                                        containerColor = Color(0xFFE5E5E5)
                                    )
                                )
                            }
                        }

                        // Send Request Button with Gradient
                        Button(
                            onClick = {
                                if (message.isNotBlank() && selectedChildId > 0) {
                                    scope.launch {
                                        assistanceRepo.createRequest(selectedChildId, message, priority)
                                            .onSuccess {
                                                message = ""
                                                priority = "normal"
                                                // Refresh requests
                                                assistanceRepo.getRequests()
                                                    .onSuccess { requestsList ->
                                                        requests = requestsList
                                                    }
                                            }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = message.isNotBlank() && selectedChildId > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(CalmBlue, CalmPurple)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Send Request",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Previous Messages Card
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
                            text = "Previous Messages",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (requests.isEmpty()) {
                            Text(
                                text = "No messages yet. Send your first message to the care team above.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        } else {
                            requests.forEach { request ->
                                MessageItem(request = request)
                                if (request != requests.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(request: AssistanceRequest) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status and Date Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (request.status) {
                    "in-progress" -> CalmBlue.copy(alpha = 0.2f)
                    "resolved" -> CalmGreen.copy(alpha = 0.2f)
                    else -> Color(0xFFE5E5E5)
                }
            ) {
                Text(
                    text = request.status.replace("-", " "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = when (request.status) {
                        "in-progress" -> CalmBlue
                        "resolved" -> CalmGreen
                        else -> TextSecondary
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Date
            Text(
                text = formatDate(request.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Message Content
        Text(
            text = request.message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )

        // Response if available
        request.notes?.let { notes ->
            if (notes.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Response from ${request.handledByName ?: "Care Team"}:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
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

