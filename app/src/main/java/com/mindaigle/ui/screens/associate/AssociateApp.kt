package com.mindaigle.ui.screens.associate

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
fun AssociateApp(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    // Refresh key that changes when tab changes - triggers refresh on navigation
    var refreshKey by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Students") },
                    label = { Text("Students") },
                    selected = selectedTab == 0,
                    onClick = { 
                        selectedTab = 0
                        refreshKey++ // Trigger refresh when navigating to this tab
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Warning, contentDescription = "Alerts") },
                    label = { Text("Alerts", maxLines = 1) },
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        refreshKey++ // Trigger refresh when navigating to this tab
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = "Chat") },
                    label = { Text("Chat", maxLines = 1) },
                    selected = selectedTab == 2,
                    onClick = { 
                        selectedTab = 2
                        refreshKey++ // Trigger refresh when navigating to this tab
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    selected = selectedTab == 3,
                    onClick = { 
                        selectedTab = 3
                        refreshKey++ // Trigger refresh when navigating to this tab
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "Screeners") },
                    label = { Text("Screeners", maxLines = 1) },
                    selected = selectedTab == 4,
                    onClick = { 
                        selectedTab = 4
                        refreshKey++ // Trigger refresh when navigating to this tab
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = "Messages") },
                    label = { Text("Messages") },
                    selected = selectedTab == 5,
                    onClick = { 
                        selectedTab = 5
                        refreshKey++ // Trigger refresh when navigating to this tab
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Resources") },
                    label = { Text("Resources", maxLines = 1) },
                    selected = selectedTab == 6,
                    onClick = { selectedTab = 6 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Wearable") },
                    label = { Text("Wearable", maxLines = 1) },
                    selected = selectedTab == 7,
                    onClick = { selectedTab = 7 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CalmBlue,
                        selectedTextColor = CalmBlue,
                        indicatorColor = CalmBlue.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 8,
                    onClick = { selectedTab = 8 },
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
                0 -> AssociateStudents(userName = userName, refreshKey = refreshKey, onNavigateToTab = onNavigateToTab)
                1 -> AssociateAlerts(userName = userName, refreshKey = refreshKey, onNavigateToTab = onNavigateToTab)
                2 -> AssociateRequests(userName = userName, refreshKey = refreshKey, onNavigateToTab = onNavigateToTab)
                3 -> AssociateAnalytics(userName = userName, refreshKey = refreshKey, onNavigateToTab = onNavigateToTab)
                4 -> AssociateScreeners(userName = userName, refreshKey = refreshKey, onNavigateToTab = onNavigateToTab)
                5 -> AssociateCommunications(userName = userName, refreshKey = refreshKey, onNavigateToTab = onNavigateToTab)
                6 -> CommunityResourcesScreen(userName = userName)
                7 -> WearableSyncScreen(userName = userName)
                8 -> AssociateSettings(userName = userName, userEmail = userEmail, onLogout = onLogout, onNavigateToTab = onNavigateToTab)
            }
        }
    }
}

