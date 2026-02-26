package com.mindaigle.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mindaigle.data.remote.dto.Achievement
import com.mindaigle.data.repository.AchievementRepository
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAchievements(
    userName: String,
    onBack: (() -> Unit)? = null
) {
    var achievementsData by remember { mutableStateOf<com.mindaigle.data.remote.dto.AchievementsData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val achievementRepo = remember { AchievementRepository() }

    fun loadAchievements() {
        isLoading = true
        scope.launch {
            achievementRepo.getComputedAchievements(30) // Get last 30 days
                .onSuccess {
                    achievementsData = it
                    isLoading = false
                    if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("StudentAchievements", "Loaded achievements: ${it.achievements.size} total, ${it.achievements.count { a -> a.unlocked }} unlocked, ${it.points} points, ${it.streak} day streak")
                }
                .onFailure { error ->
                    isLoading = false
                    android.util.Log.e("StudentAchievements", "Failed to load achievements", error)
                }
        }
    }

    LaunchedEffect(Unit) {
        loadAchievements()
        // Auto-refresh every 30 seconds
        while (true) {
            kotlinx.coroutines.delay(30000)
            loadAchievements()
        }
    }

    val achievements = achievementsData?.achievements ?: emptyList()
    val unlockedCount = achievements.count { it.unlocked }
    val totalPoints = achievementsData?.points ?: 0
    val streak = achievementsData?.streak ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Achievements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { loadAchievements() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh achievements")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )

            // Stats Card - iOS-style with gradients
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6C5CE7),
                                        Color(0xFF8B7EE8),
                                        Color(0xFFA29BFE)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            VibrantStatItem("🏆", "Unlocked", "$unlockedCount/${achievements.size}")
                            VibrantStatItem("⭐", "Points", "$totalPoints")
                            VibrantStatItem("🔥", "Streak", "${streak} days")
                        }
                    }
                }
            }

            if (isLoading && achievements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CalmBlue)
                }
            } else if (achievements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary
                        )
                        Text(
                            text = "No achievements yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Complete activity logs to unlock achievements!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(achievements) { achievement ->
                        VibrantAchievementCardFull(achievement = achievement)
                    }
                }
            }
        }
    }
}

@Composable
fun VibrantStatItem(emoji: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun VibrantAchievementCardFull(achievement: com.mindaigle.data.remote.dto.ComputedAchievement) {
    // Same gradient logic as home screen
    val gradientColors = when {
        achievement.title.contains("First", ignoreCase = true) -> listOf(
            Color(0xFFFF6B6B), Color(0xFFFF8E8E)
        )
        achievement.title.contains("Week", ignoreCase = true) -> listOf(
            Color(0xFF4ECDC4), Color(0xFF6EDDD6)
        )
        achievement.title.contains("Month", ignoreCase = true) -> listOf(
            Color(0xFFFFD93D), Color(0xFFFFE66D)
        )
        achievement.title.contains("Step", ignoreCase = true) -> listOf(
            Color(0xFF95E1D3), Color(0xFFB4F0E2)
        )
        achievement.title.contains("Sleep", ignoreCase = true) -> listOf(
            Color(0xFFA8E6CF), Color(0xFFC8F5DD)
        )
        achievement.title.contains("Hydration", ignoreCase = true) -> listOf(
            Color(0xFF6C5CE7), Color(0xFF8B7EE8)
        )
        achievement.title.contains("Streak", ignoreCase = true) -> listOf(
            Color(0xFFFF7675), Color(0xFFFF9998)
        )
        else -> listOf(Color(0xFF74B9FF), Color(0xFF95C9FF))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.unlocked) 8.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 0f)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large emoji icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = achievement.icon,
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                
                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = achievement.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (achievement.unlocked) {
                            Text(
                                text = "✨",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    // Progress bar for locked
                    if (!achievement.unlocked && achievement.progress > 0) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { achievement.progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "${achievement.progress}% complete",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
