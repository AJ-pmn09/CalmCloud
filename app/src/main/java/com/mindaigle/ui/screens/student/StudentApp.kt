package com.mindaigle.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mindaigle.ui.screens.student.StudentHome
import com.mindaigle.ui.screens.student.StudentCheckIn
import com.mindaigle.ui.screens.student.StudentTrends
import com.mindaigle.ui.screens.student.StudentSettings
import com.mindaigle.ui.screens.common.CommunityResourcesScreen
import com.mindaigle.ui.screens.common.WearableSyncScreen
import com.mindaigle.ui.theme.CalmBlue

@Composable
fun StudentApp(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAchievements by remember { mutableStateOf(false) }
    var showStaff by remember { mutableStateOf(false) }

    val onNavigateToTab: (Int) -> Unit = { selectedTab = it }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Check-in") },
                    label = { Text("Check-in") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Trends") },
                    label = { Text("Trends") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Appointments") },
                    label = { 
                        Text(
                            "Appointments",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = "Messages") },
                    label = { 
                        Text(
                            "Messages",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Resources") },
                    label = { Text("Resources", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Wearable") },
                    label = { Text("Wearable", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    selected = selectedTab == 6,
                    onClick = { selectedTab = 6 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 7,
                    onClick = { selectedTab = 7 },
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
            when {
                showAchievements -> StudentAchievements(
                    userName = userName,
                    onBack = { showAchievements = false }
                )
                showStaff -> StudentStaff(
                    userName = userName,
                    onBack = { showStaff = false },
                    onOpenMessages = { showStaff = false; selectedTab = 4 }
                )
                else -> when (selectedTab) {
                    0 -> StudentHome(
                        userName = userName,
                        onNavigateToTab = onNavigateToTab,
                        onOpenAchievements = { showAchievements = true },
                        onOpenStaff = { showStaff = true }
                    )
                    1 -> StudentCheckIn(
                        userName = userName,
                        onNavigateToTab = onNavigateToTab
                    )
                    2 -> StudentTrends(
                        userName = userName,
                        onNavigateToTab = onNavigateToTab
                    )
                    3 -> StudentAppointments(
                        userName = userName,
                        onNavigateToTab = onNavigateToTab
                    )
                    4 -> StudentMessages(
                        userName = userName,
                        onNavigateToTab = onNavigateToTab
                    )
                    5 -> CommunityResourcesScreen(userName = userName)
                    6 -> WearableSyncScreen(userName = userName)
                    7 -> StudentSettings(
                        userName = userName,
                        userEmail = userEmail,
                        onLogout = onLogout,
                        onOpenWearable = { selectedTab = 6 },
                        onNavigateToTab = onNavigateToTab
                    )
                }
            }
        }
    }
}

