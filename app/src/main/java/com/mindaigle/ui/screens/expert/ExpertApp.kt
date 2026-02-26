package com.mindaigle.ui.screens.expert

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mindaigle.ui.screens.common.CommunityResourcesScreen
import com.mindaigle.ui.screens.common.WearableSyncScreen
import com.mindaigle.ui.theme.CalmBlue

@Composable
fun ExpertApp(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Students") },
                    label = { Text("Students") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Resources") },
                    label = { Text("Resources") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Wearable") },
                    label = { Text("Wearable") },
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            val onNavigateToTab: (Int) -> Unit = { selectedTab = it }
            when (selectedTab) {
                0 -> ExpertDashboard(userName = userName, onNavigateToTab = onNavigateToTab)
                1 -> ExpertStudents(userName = userName, onNavigateToTab = onNavigateToTab)
                2 -> ExpertReports(userName = userName, onNavigateToTab = onNavigateToTab)
                3 -> CommunityResourcesScreen(userName = userName)
                4 -> WearableSyncScreen(userName = userName)
                5 -> ExpertSettings(userName = userName, userEmail = userEmail, onLogout = onLogout, onNavigateToTab = onNavigateToTab)
            }
        }
    }
}

