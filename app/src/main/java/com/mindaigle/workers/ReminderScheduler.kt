package com.mindaigle.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val PREFS_NAME = "reminder_scheduler"
    const val KEY_REMINDER_ENABLED = "reminder_enabled"
    const val KEY_INTERVAL_HOURS = "reminder_interval_hours"
    const val KEY_LAST_REMINDER_AT = "last_reminder_at"
    const val KEY_QUIET_START = "quiet_hours_start"
    const val KEY_QUIET_END = "quiet_hours_end"

    private const val WORK_NAME = "check_in_reminder"

    fun schedule(
        context: Context,
        enabled: Boolean,
        intervalHours: Int = 24,
        quietHoursStart: String? = null,
        quietHoursEnd: String? = null
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_REMINDER_ENABLED, enabled)
            .putInt(KEY_INTERVAL_HOURS, intervalHours.coerceIn(1, 168))
            .putString(KEY_QUIET_START, quietHoursStart?.substringBefore(":") ?: "22")
            .putString(KEY_QUIET_END, quietHoursEnd?.substringBefore(":") ?: "7")
            .apply()

        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun ensureScheduled(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
        val intervalHours = prefs.getInt(KEY_INTERVAL_HOURS, 24)
        val quietStart = prefs.getString(KEY_QUIET_START, "22")
        val quietEnd = prefs.getString(KEY_QUIET_END, "7")
        schedule(context, enabled, intervalHours, quietStart, quietEnd)
    }
}
