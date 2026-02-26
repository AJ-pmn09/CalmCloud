package com.mindaigle.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.mindaigle.data.repository.AssistanceRepository
import com.mindaigle.data.repository.StudentRepository
import com.mindaigle.data.remote.dto.AssistanceRequest
import com.mindaigle.data.remote.dto.Child
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentMessages(
    userName: String,
    onNavigateToTab: (Int) -> Unit = {}
) {
    var children by remember { mutableStateOf<List<Child>>(emptyList()) }
    var requests by remember { mutableStateOf<List<AssistanceRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("normal") }
    var selectedChildIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectAll by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val studentRepo = remember { StudentRepository() }
    val assistanceRepo = remember { AssistanceRepository() }

    LaunchedEffect(Unit) {
        // Initial load
        studentRepo.getChildren()
            .onSuccess { childrenList ->
                children = childrenList
            }
        assistanceRepo.getRequests()
            .onSuccess { requestsList ->
                requests = requestsList
                isLoading = false
            }
            .onFailure {
                isLoading = false
            }
        
        // Automatic periodic refresh every 20 seconds while screen is active
        while (true) {
            kotlinx.coroutines.delay(20000) // 20 seconds
            studentRepo.getChildren()
                .onSuccess { childrenList ->
                    children = childrenList
                }
            assistanceRepo.getRequests()
                .onSuccess { requestsList ->
                    requests = requestsList
                }
        }
    }

    // Update selected children when selectAll changes
    LaunchedEffect(selectAll) {
        selectedChildIds = if (selectAll) {
            children.map { it.id }.toSet()
        } else {
            emptySet()
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
                        text = "Messages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateToTab(0) }) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToTab(5) }) {
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

                        // Child Selection
                        Text(
                            text = "Select Child(ren)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )

                        // Select All Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectAll = !selectAll },
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectAll,
                                onCheckedChange = { selectAll = it }
                            )
                            Text(
                                text = "Select All Children",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Individual Child Selection
                        if (children.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                children.forEach { child ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedChildIds = if (selectedChildIds.contains(child.id)) {
                                                    selectedChildIds - child.id
                                                } else {
                                                    selectedChildIds + child.id
                                                }
                                                selectAll = selectedChildIds.size == children.size
                                            },
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedChildIds.contains(child.id),
                                            onCheckedChange = {
                                                selectedChildIds = if (it) {
                                                    selectedChildIds + child.id
                                                } else {
                                                    selectedChildIds - child.id
                                                }
                                                selectAll = selectedChildIds.size == children.size
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = child.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (child.grade != null) {
                                                Text(
                                                    text = "Grade ${child.grade}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No children linked to your account",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

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
                                if (message.isNotBlank() && (selectedChildIds.isNotEmpty() || selectAll)) {
                                    scope.launch {
                                        val studentIds = if (selectAll || selectedChildIds.isEmpty()) {
                                            "all"
                                        } else if (selectedChildIds.size == 1) {
                                            selectedChildIds.first()
                                        } else {
                                            selectedChildIds.toList()
                                        }
                                        assistanceRepo.createRequest(studentIds, message, priority)
                                            .onSuccess {
                                                message = ""
                                                priority = "normal"
                                                selectedChildIds = emptySet()
                                                selectAll = false
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
                            enabled = message.isNotBlank() && (selectedChildIds.isNotEmpty() || selectAll),
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

