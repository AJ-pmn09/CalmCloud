package com.mindaigle.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.ServerConfig
import com.mindaigle.data.repository.AuthRepository
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (role: String, userName: String) -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToWebSocketTest: (() -> Unit)? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var connectionTestMessage by remember { mutableStateOf<String?>(null) }
    var connectionTestRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = remember { AuthRepository() }

    val alpha by animateFloatAsState(
        targetValue = if (isLoading) 0.6f else 1f,
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // App Logo/Title
            Text(
                text = "MindAIgle",
                style = MaterialTheme.typography.displayLarge,
                color = CalmBlue,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your Mental Health Companion",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Welcome Back!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight
                        )
                    )

                    // Error Message
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Login Button
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill in all fields"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                repository.login(email, password)
                                    .onSuccess { response ->
                                        val u = response.resolvedUser()
                                        val role = u?.role ?: "student"
                                        val userName = u?.displayName() ?: "User"
                                        onLoginSuccess(role, userName)
                                    }
                                    .onFailure { e ->
                                        errorMessage = e.message ?: "Login failed"
                                        isLoading = false
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalmBlue
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = TextOnPrimary
                            )
                        } else {
                            Text(
                                text = "Login",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Signup Link
                    TextButton(
                        onClick = onNavigateToSignup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Don't have an account? Sign Up",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CalmBlue
                        )
                    }

                    // Test connection (3×) — pings server so you can confirm app connects
                    TextButton(
                        onClick = {
                            connectionTestMessage = null
                            connectionTestRunning = true
                            scope.launch {
                                var successCount = 0
                                repeat(3) { attempt ->
                                    val ok = withContext(Dispatchers.IO) {
                                        try {
                                            val r = ApiClient.api.hello()
                                            r.isSuccessful
                                        } catch (_: Throwable) {
                                            false
                                        }
                                    }
                                    if (ok) successCount++
                                    connectionTestMessage = if (attempt < 2) "Testing ${attempt + 1}/3…" else null
                                    if (attempt < 2) delay(400)
                                }
                                connectionTestRunning = false
                                connectionTestMessage = when (successCount) {
                                    3 -> "Connection OK (3/3) — ${ServerConfig.getCurrentConfig()}"
                                    2 -> "Connection 2/3 — ${ServerConfig.getCurrentConfig()}"
                                    1 -> "Connection 1/3 — check server"
                                    else -> "Server unreachable (0/3). Check: ${ServerConfig.getCurrentConfig()}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !connectionTestRunning
                    ) {
                        Text(
                            text = if (connectionTestRunning) "Testing connection…" else "Test connection (3×)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    connectionTestMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (msg.startsWith("Connection OK") || msg.startsWith("Connection 2")) CalmGreen else TextSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Test profiles: use app without server (no login required)
            Text(
                text = "Test without server",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TestProfileButtons(
                onSelect = { role, userName, email ->
                    AuthManager.saveTestProfile(role, userName, email)
                    onLoginSuccess(role, userName)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // WebSocket Test Button (only shown in debug builds)
            onNavigateToWebSocketTest?.let {
                if (com.mindaigle.data.remote.ServerConfig.ENABLE_DEBUG_CONFIG) {
                    TextButton(
                        onClick = it,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "WebSocket Test (Debug)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private data class TestProfile(val role: String, val displayName: String, val email: String)

@Composable
private fun TestProfileButtons(onSelect: (role: String, userName: String, email: String) -> Unit) {
    val testProfiles = listOf(
        TestProfile("student", "Student One", "student1@test.mindaigle"),
        TestProfile("student", "Student Two", "student2@test.mindaigle"),
        TestProfile("student", "Student Three", "student3@test.mindaigle"),
        TestProfile("parent", "Parent One", "parent1@test.mindaigle"),
        TestProfile("parent", "Parent Two", "parent2@test.mindaigle"),
        TestProfile("parent", "Parent Three", "parent3@test.mindaigle"),
        TestProfile("associate", "Associate One", "associate1@test.mindaigle"),
        TestProfile("associate", "Associate Two", "associate2@test.mindaigle"),
        TestProfile("associate", "Associate Three", "associate3@test.mindaigle"),
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        testProfiles.chunked(3).forEach { rowProfiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                rowProfiles.forEach { p ->
                    OutlinedButton(
                        onClick = { onSelect(p.role, p.displayName, p.email) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmBlue)
                    ) {
                        Text(
                            text = p.displayName.replace(" ", "\n"),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

