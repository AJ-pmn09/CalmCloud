package com.mindaigle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.ServerConfig
import com.mindaigle.data.remote.WebSocketManager
import com.mindaigle.ui.theme.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSocketTestScreen() {
    val context = LocalContext.current
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    var isConnected by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<String>>(emptyList()) }
    var testMessage by remember { mutableStateOf("") }
    var lastEvent by remember { mutableStateOf<String?>(null) }
    var lastEventData by remember { mutableStateOf<String?>(null) }
    
    // Server configuration
    var serverUrl by remember { mutableStateOf(ServerConfig.getBaseUrl().replace(":${ServerConfig.getPort()}", "")) }
    var serverPort by remember { mutableStateOf(ServerConfig.getPort()) }
    var showServerConfig by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    // Function to add messages (defined before LaunchedEffect so it can be captured)
    val addMessage: (String) -> Unit = { message ->
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        messages = messages + "[$timestamp] $message"
    }
    
    // Connection status listener
    LaunchedEffect(Unit) {
        WebSocketManager.addConnectionListener { connected ->
            isConnected = connected
            connectionStatus = if (connected) "Connected" else "Disconnected"
            addMessage("Connection status: $connectionStatus")
        }
        
        // Event listeners
        WebSocketManager.addEventListener("test_response") { data ->
            val jsonData = data as? JSONObject
            lastEvent = "test_response"
            lastEventData = jsonData?.toString(2) ?: data.toString()
            addMessage("Test response received: ${jsonData?.toString()}")
        }
        
        WebSocketManager.addEventListener("new_alert") { data ->
            val jsonData = data as? JSONObject
            lastEvent = "new_alert"
            lastEventData = jsonData?.toString(2) ?: data.toString()
            addMessage("New alert received!")
        }
        
        WebSocketManager.addEventListener("alert_created") { data ->
            val jsonData = data as? JSONObject
            lastEvent = "alert_created"
            lastEventData = jsonData?.toString(2) ?: data.toString()
            addMessage("Alert created confirmation received!")
        }
        
        WebSocketManager.addEventListener("connected") { data ->
            val jsonData = data as? JSONObject
            lastEvent = "connected"
            lastEventData = jsonData?.toString(2) ?: data.toString()
            addMessage("Server connection confirmed")
        }
        
        // Connect on screen load
        val token = AuthManager.getToken()
        WebSocketManager.connect(token)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Don't disconnect here - keep connection alive
            // WebSocketManager.disconnect()
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
                .verticalScroll(scrollState)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "WebSocket Test",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Server Configuration Card (only shown in debug builds)
                if (ServerConfig.ENABLE_DEBUG_CONFIG) {
                    Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
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
                            Text(
                                text = "Server Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showServerConfig = !showServerConfig }) {
                                Icon(
                                    imageVector = if (showServerConfig) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle config"
                                )
                            }
                        }
                        
                        Text(
                            text = "Current: ${ServerConfig.getCurrentConfig()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        
                        if (showServerConfig) {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { 
                                    serverUrl = it
                                    // Auto-detect HTTPS tunnel URLs and clear port
                                    if (it.startsWith("https://")) {
                                        serverPort = ""
                                    }
                                },
                                label = { Text("Server URL") },
                                placeholder = { Text("http://192.168.1.100 or https://tunnel-url.loca.lt") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = { 
                                    Text(
                                        if (serverUrl.startsWith("https://")) 
                                            "HTTPS detected - port not needed" 
                                        else 
                                            "For tunnel URLs, use: https://tunnel-url.loca.lt"
                                    )
                                }
                            )
                            
                            OutlinedTextField(
                                value = serverPort,
                                onValueChange = { serverPort = it },
                                label = { Text("Port (leave empty for HTTPS)") },
                                placeholder = { Text("3003 or leave empty for HTTPS") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !serverUrl.startsWith("https://")
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        // Handle HTTPS URLs (tunnel) - no port needed
                                        val finalPort = if (serverUrl.startsWith("https://")) "" else serverPort
                                        ServerConfig.setServerUrl(context, serverUrl)
                                        ServerConfig.setPort(context, finalPort)
                                        addMessage("✅ Server URL updated to: ${ServerConfig.getCurrentConfig()}")
                                        addMessage("🔄 Reconnecting with new URL...")
                                        // Disconnect and reconnect with new URL
                                        WebSocketManager.disconnect()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                                ) {
                                    Text("Save & Reconnect")
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        ServerConfig.resetToDefaults(context)
                                        serverUrl = ServerConfig.getBaseUrl().replace(":${ServerConfig.getPort()}", "")
                                        serverPort = ServerConfig.getPort()
                                        addMessage("Reset to default server")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
                }
                
                // Server Info Card (always shown)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CalmBlue.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Server Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connected to: ${ServerConfig.getCurrentConfig()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        if (ServerConfig.isUsingCustomConfig()) {
                            Text(
                                text = "Using custom configuration (debug mode)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800) // Orange color
                            )
                        } else {
                            Text(
                                text = "Using production server configuration",
                                style = MaterialTheme.typography.bodySmall,
                                color = CalmGreen
                            )
                        }
                    }
                }
                
                // Connection Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) CalmGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Connection Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = connectionStatus,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isConnected) CalmGreen else Color.Red
                            )
                        }
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isConnected) CalmGreen else Color.Red,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val token = AuthManager.getToken()
                            WebSocketManager.connect(token)
                            addMessage("Attempting to connect...")
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isConnected,
                        colors = ButtonDefaults.buttonColors(containerColor = CalmGreen)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                    
                    Button(
                        onClick = {
                            WebSocketManager.disconnect()
                            addMessage("Disconnected")
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                }
                
                // Test Message Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Send Test Message",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = testMessage,
                            onValueChange = { testMessage = it },
                            label = { Text("Test Message") },
                            placeholder = { Text("Enter test message...") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isConnected
                        )
                        
                        Button(
                            onClick = {
                                WebSocketManager.sendTest(testMessage)
                                addMessage("Sent test message: $testMessage")
                                testMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isConnected && testMessage.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Test")
                        }
                    }
                }
                
                // Last Event Data
                if (lastEvent != null && lastEventData != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Last Event: $lastEvent",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = lastEventData ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .padding(12.dp)
                            )
                        }
                    }
                }
                
                // Messages Log
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
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
                            Text(
                                text = "Messages Log",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { messages = emptyList() }
                            ) {
                                Text("Clear")
                            }
                        }
                        
                        if (messages.isEmpty()) {
                            Text(
                                text = "No messages yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            messages.takeLast(20).forEach { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

