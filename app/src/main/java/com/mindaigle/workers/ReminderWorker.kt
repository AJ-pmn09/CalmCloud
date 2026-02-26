package com.mindaigle.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindaigle.MainActivity
import java.util.Calendar

const val REMINDER_CHANNEL_ID = "check_in_reminders"

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences(ReminderScheduler.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(ReminderScheduler.KEY_REMINDER_ENABLED, false)) return Result.success()
        if (isInQuietHours(prefs)) return Result.success()

        val lastShown = prefs.getLong(ReminderScheduler.KEY_LAST_REMINDER_AT, 0L)
        val intervalHours = prefs.getInt(ReminderScheduler.KEY_INTERVAL_HOURS, 24).coerceAtLeast(1)
        if (lastShown > 0 && (System.currentTimeMillis() - lastShown) < intervalHours * 3600_000L) {
            return Result.success()
        }

        createChannelIfNeeded()
        val openApp = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = PendingIntent.getActivity(
            context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MindAigle")
            .setContentText("Time for your wellness check-in 💙")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(REMINDER_NOTIFICATION_ID, notification)
        prefs.edit().putLong(ReminderScheduler.KEY_LAST_REMINDER_AT, System.currentTimeMillis()).apply()
        return Result.success()
    }

    private fun isInQuietHours(prefs: android.content.SharedPreferences): Boolean {
        val start = prefs.getString(ReminderScheduler.KEY_QUIET_START, "22") ?: "22"
        val end = prefs.getString(ReminderScheduler.KEY_QUIET_END, "7") ?: "7"
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val startH = start.substringBefore(":").toIntOrNull() ?: 22
        val endH = end.substringBefore(":").toIntOrNull() ?: 7
        if (startH <= endH) return hour in startH until endH
        return hour >= startH || hour < endH
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(REMINDER_CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Check-in Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    companion object {
        const val REMINDER_NOTIFICATION_ID = 1001
    }
}
