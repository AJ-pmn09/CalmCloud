package com.mindaigle

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mindaigle.data.repository.AuthRepository
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.BuildConfig
import com.mindaigle.data.remote.ServerConfig
import com.mindaigle.ui.screens.*
import com.mindaigle.ui.theme.MindAigleTheme
import kotlinx.coroutines.launch

sealed class Screen {
    object Login : Screen()
    object Signup : Screen()
    object WebSocketTest : Screen()
    data class Dashboard(val role: String, val userName: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ServerConfig and AuthManager are initialized in MindAigleApplication
        val apiBaseUrl = ServerConfig.getApiBaseUrl()
        val baseUrl = ServerConfig.getBaseUrl()
        if (BuildConfig.DEBUG) Log.d("MindAigle", "Server: $baseUrl -> $apiBaseUrl")
        enableEdgeToEdge()
        setContent {
            MindAigleTheme {
                MindAigleApp()
            }
        }
    }
}

@Composable
fun MindAigleApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    // Check if user is already authenticated (real token or test profile)
    LaunchedEffect(Unit) {
        try {
            if (AuthManager.isTestMode()) {
                val role = AuthManager.getTestRole() ?: "student"
                val name = AuthManager.getTestUserName() ?: "Test User"
                currentScreen = Screen.Dashboard(role, name)
                return@LaunchedEffect
            }
            if (AuthManager.getToken() != null) {
                authRepository.getProfile()
                    .onSuccess { user ->
                        currentScreen = Screen.Dashboard(user.role, user.name ?: "User")
                    }
                    .onFailure {
                        currentScreen = Screen.Login
                    }
            }
        } catch (e: Throwable) {
            Log.e("MindAIgle", "Auth check failed", e)
            currentScreen = Screen.Login
        }
    }

    when (val screen = currentScreen) {
        is Screen.Login -> {
            LoginScreen(
                onLoginSuccess = { role, userName ->
                    currentScreen = Screen.Dashboard(role, userName)
                },
                onNavigateToSignup = {
                    currentScreen = Screen.Signup
                },
                onNavigateToWebSocketTest = {
                    currentScreen = Screen.WebSocketTest
                }
            )
        }
        is Screen.WebSocketTest -> {
            WebSocketTestScreen()
        }
        is Screen.Signup -> {
            SignupScreen(
                onSignupSuccess = { role, userName ->
                    currentScreen = Screen.Dashboard(role, userName)
                },
                onNavigateToLogin = {
                    currentScreen = Screen.Login
                }
            )
        }
        is Screen.Dashboard -> {
            when (screen.role) {
                "student" -> {
                    var userEmail by remember { mutableStateOf(AuthManager.getTestUserEmail() ?: "") }
                    LaunchedEffect(Unit) {
                        if (!AuthManager.isTestMode()) {
                            authRepository.getProfile()
                                .onSuccess { user -> userEmail = user.email }
                        }
                    }
                    com.mindaigle.ui.screens.student.StudentApp(
                        userName = screen.userName,
                        userEmail = userEmail,
                        onLogout = {
                            authRepository.logout()
                            currentScreen = Screen.Login
                        }
                    )
                }
                "parent" -> {
                    var userEmail by remember { mutableStateOf(AuthManager.getTestUserEmail() ?: "") }
                    LaunchedEffect(Unit) {
                        if (!AuthManager.isTestMode()) {
                            authRepository.getProfile()
                                .onSuccess { user -> userEmail = user.email }
                        }
                    }
                    com.mindaigle.ui.screens.parent.ParentApp(
                        userName = screen.userName,
                        userEmail = userEmail,
                        onLogout = {
                            authRepository.logout()
                            currentScreen = Screen.Login
                        }
                    )
                }
                "associate" -> {
                    var userEmail by remember { mutableStateOf(AuthManager.getTestUserEmail() ?: "") }
                    LaunchedEffect(Unit) {
                        if (!AuthManager.isTestMode()) {
                            authRepository.getProfile()
                                .onSuccess { user -> userEmail = user.email }
                        }
                    }
                    com.mindaigle.ui.screens.associate.AssociateApp(
                        userName = screen.userName,
                        userEmail = userEmail,
                        onLogout = {
                            authRepository.logout()
                            currentScreen = Screen.Login
                        }
                    )
                }
                "expert" -> {
                    var userEmail by remember { mutableStateOf(AuthManager.getTestUserEmail() ?: "") }
                    LaunchedEffect(Unit) {
                        if (!AuthManager.isTestMode()) {
                            authRepository.getProfile()
                                .onSuccess { user -> userEmail = user.email }
                        }
                    }
                    com.mindaigle.ui.screens.expert.ExpertApp(
                        userName = screen.userName,
                        userEmail = userEmail,
                        onLogout = {
                            authRepository.logout()
                            currentScreen = Screen.Login
                        }
                    )
                }
                else -> {
                    // Fallback to login
                    currentScreen = Screen.Login
                }
            }
        }
    }
}
