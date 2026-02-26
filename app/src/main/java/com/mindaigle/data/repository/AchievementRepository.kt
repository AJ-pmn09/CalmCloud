package com.mindaigle.data.repository

import com.mindaigle.data.remote.ApiClient
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.TestMockData
import com.mindaigle.data.remote.dto.*
import java.text.SimpleDateFormat
import java.util.*

class AchievementRepository {
    private val api = ApiClient.api

    suspend fun getActivityLogs(days: Int = 30): Result<List<ActivityLog>> {
        if (AuthManager.isTestMode()) {
            val res = TestMockData.mockActivityLogsResponse()
            return Result.success(res.logs)
        }
        return try {
            val response = api.getActivityLogs(
                "Bearer ${com.mindaigle.data.remote.AuthManager.getToken()}",
                days
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                // Log for debugging
                if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("AchievementRepo", "Activity logs: requested $days days, got ${body.logs.size} logs")
                body.meta?.let { meta ->
                    if (com.mindaigle.BuildConfig.DEBUG) android.util.Log.d("AchievementRepo", "Meta: totalInDb=${meta.totalInDb}, dateRange=${meta.dateRange?.earliest} to ${meta.dateRange?.latest}")
                }
                Result.success(body.logs)
            } else {
                android.util.Log.e("AchievementRepo", "Failed to get activity logs: ${response.message()}")
                Result.failure(Exception(response.message() ?: "Failed to get activity logs"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AchievementRepo", "Exception getting activity logs", e)
            Result.failure(e)
        }
    }

    /**
     * Calculate points from activity logs (matches dashboard logic)
     */
    fun calculatePoints(logs: List<ActivityLog>): Int {
        var points = 0
        
        logs.forEach { log ->
            // Steps: 10 points per 1,000 steps
            points += (log.steps / 1000) * 10
            
            // Sleep
            when {
                log.sleepHours >= 8 -> points += 20
                log.sleepHours >= 7 -> points += 15
                log.sleepHours >= 6 -> points += 10
            }
            
            // Hydration
            when {
                log.hydrationPercent >= 80 -> points += 15
                log.hydrationPercent >= 50 -> points += 10
            }
            
            // Nutrition
            when {
                log.nutritionPercent >= 80 -> points += 15
                log.nutritionPercent >= 50 -> points += 10
            }
            
            // Daily check-in: +5 per log entry
            points += 5
        }
        
        return points
    }

    /**
     * Calculate streak from activity logs (matches dashboard logic)
     */
    fun calculateStreak(logs: List<ActivityLog>): Int {
        if (logs.isEmpty()) return 0
        
        // Sort by date descending
        val sortedLogs = logs.sortedByDescending { it.date }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        // Start from most recent log date
        val mostRecentDate = dateFormat.parse(sortedLogs[0].date) ?: return 0
        calendar.time = mostRecentDate
        
        var streak = 0
        val logDates = sortedLogs.map { it.date }.toSet()
        
        // Check consecutive days starting from most recent
        while (true) {
            val dateStr = dateFormat.format(calendar.time)
            if (logDates.contains(dateStr)) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        
        return streak
    }

    /**
     * Calculate achievements from activity logs (matches dashboard logic)
     */
    fun calculateAchievements(logs: List<ActivityLog>, streak: Int): List<ComputedAchievement> {
        val achievements = mutableListOf<ComputedAchievement>()
        
        // First Steps: logs >= 1
        val firstStepsProgress = (logs.size.toFloat() / 1f * 100f).toInt().coerceIn(0, 100)
        achievements.add(
            ComputedAchievement(
                title = "First Steps",
                description = "Complete your first activity log",
                icon = "👣",
                unlocked = logs.size >= 1,
                progress = firstStepsProgress
            )
        )
        
        // Week Warrior: logs >= 7
        val weekWarriorProgress = (logs.size.toFloat() / 7f * 100f).toInt().coerceIn(0, 100)
        achievements.add(
            ComputedAchievement(
                title = "Week Warrior",
                description = "Complete 7 activity logs",
                icon = "📅",
                unlocked = logs.size >= 7,
                progress = weekWarriorProgress
            )
        )
        
        // Monthly Master: logs >= 30
        val monthlyMasterProgress = (logs.size.toFloat() / 30f * 100f).toInt().coerceIn(0, 100)
        achievements.add(
            ComputedAchievement(
                title = "Monthly Master",
                description = "Complete 30 activity logs",
                icon = "📆",
                unlocked = logs.size >= 30,
                progress = monthlyMasterProgress
            )
        )
        
        // Step Champion: any day steps >= 10,000
        val hasStepChampion = logs.any { it.steps >= 10000 }
        achievements.add(
            ComputedAchievement(
                title = "Step Champion",
                description = "Reach 10,000 steps in a day",
                icon = "🏃",
                unlocked = hasStepChampion,
                progress = if (hasStepChampion) 100 else 0
            )
        )
        
        // Sleep Master: any day sleep_hours >= 8
        val hasSleepMaster = logs.any { it.sleepHours >= 8 }
        achievements.add(
            ComputedAchievement(
                title = "Sleep Master",
                description = "Get 8+ hours of sleep in a day",
                icon = "😴",
                unlocked = hasSleepMaster,
                progress = if (hasSleepMaster) 100 else 0
            )
        )
        
        // Hydration Hero: any day hydration_percent >= 80
        val hasHydrationHero = logs.any { it.hydrationPercent >= 80 }
        achievements.add(
            ComputedAchievement(
                title = "Hydration Hero",
                description = "Reach 80% hydration in a day",
                icon = "💧",
                unlocked = hasHydrationHero,
                progress = if (hasHydrationHero) 100 else 0
            )
        )
        
        // Streak Starter: streak >= 3
        val streakStarterProgress = (streak.toFloat() / 3f * 100f).toInt().coerceIn(0, 100)
        achievements.add(
            ComputedAchievement(
                title = "Streak Starter",
                description = "Maintain a 3-day activity streak",
                icon = "🔥",
                unlocked = streak >= 3,
                progress = streakStarterProgress
            )
        )
        
        // Streak Star: streak >= 7
        val streakStarProgress = (streak.toFloat() / 7f * 100f).toInt().coerceIn(0, 100)
        achievements.add(
            ComputedAchievement(
                title = "Streak Star",
                description = "Maintain a 7-day activity streak",
                icon = "⭐",
                unlocked = streak >= 7,
                progress = streakStarProgress
            )
        )
        
        return achievements
    }

    /**
     * Get computed achievements data (matches dashboard logic)
     */
    suspend fun getComputedAchievements(days: Int = 30): Result<AchievementsData> {
        return try {
            val logsResult = getActivityLogs(days)
            if (logsResult.isFailure) {
                return Result.failure(logsResult.exceptionOrNull() ?: Exception("Failed to get logs"))
            }
            
            val logs = logsResult.getOrThrow()
            val points = calculatePoints(logs)
            val streak = calculateStreak(logs)
            val achievements = calculateAchievements(logs, streak)
            
            Result.success(
                AchievementsData(
                    points = points,
                    streak = streak,
                    achievements = achievements
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

