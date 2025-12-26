package com.example.calmcloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.calmcloud.data.repository.AuthRepository
import com.example.calmcloud.data.remote.AuthManager
import com.example.calmcloud.ui.screens.*
import com.example.calmcloud.ui.theme.CalmCloudTheme
import kotlinx.coroutines.launch

sealed class Screen {
    object Login : Screen()
    object Signup : Screen()
    data class Dashboard(val role: String, val userName: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalmCloudTheme {
                CalmCloudApp()
            }
        }
    }
}

@Composable
fun CalmCloudApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    // Check if user is already authenticated
    LaunchedEffect(Unit) {
        if (AuthManager.getToken() != null) {
            authRepository.getProfile()
                .onSuccess { user ->
                    currentScreen = Screen.Dashboard(user.role, user.name ?: "User")
                }
                .onFailure {
                    // Token invalid, show login
                    currentScreen = Screen.Login
                }
        }
    }

    when (val screen = currentScreen) {
        is Screen.Login -> {
            LoginScreen(
                onLoginSuccess = { role ->
                    scope.launch {
                        authRepository.getProfile()
                            .onSuccess { user ->
                                currentScreen = Screen.Dashboard(role, user.name ?: "User")
                            }
                    }
                },
                onNavigateToSignup = {
                    currentScreen = Screen.Signup
                }
            )
        }
        is Screen.Signup -> {
            SignupScreen(
                onSignupSuccess = { role ->
                    scope.launch {
                        authRepository.getProfile()
                            .onSuccess { user ->
                                currentScreen = Screen.Dashboard(role, user.name ?: "User")
                            }
                    }
                },
                onNavigateToLogin = {
                    currentScreen = Screen.Login
                }
            )
        }
        is Screen.Dashboard -> {
            when (screen.role) {
                "student" -> {
                    var userEmail by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        authRepository.getProfile()
                            .onSuccess { user ->
                                userEmail = user.email
                            }
                    }
                    com.example.calmcloud.ui.screens.student.StudentApp(
                        userName = screen.userName,
                        userEmail = userEmail,
                        onLogout = {
                            authRepository.logout()
                            currentScreen = Screen.Login
                        }
                    )
                }
                "parent" -> {
                    var userEmail by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        authRepository.getProfile()
                            .onSuccess { user ->
                                userEmail = user.email
                            }
                    }
                    com.example.calmcloud.ui.screens.parent.ParentApp(
                        userName = screen.userName,
                        userEmail = userEmail,
                        onLogout = {
                            authRepository.logout()
                            currentScreen = Screen.Login
                        }
                    )
                }
                "associate" -> {
                    var userEmail by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        authRepository.getProfile()
                            .onSuccess { user ->
                                userEmail = user.email
                            }
                    }
                    com.example.calmcloud.ui.screens.associate.AssociateApp(
                        userName = screen.userName,
                        userEmail = userEmail,
                        onLogout = {
                            authRepository.logout()
                            currentScreen = Screen.Login
                        }
                    )
                }
                "expert" -> {
                    var userEmail by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        authRepository.getProfile()
                            .onSuccess { user ->
                                userEmail = user.email
                            }
                    }
                    com.example.calmcloud.ui.screens.expert.ExpertApp(
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
