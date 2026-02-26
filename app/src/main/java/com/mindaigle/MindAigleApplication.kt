package com.mindaigle

import android.app.Application
import android.util.Log
import com.mindaigle.data.remote.AuthManager
import com.mindaigle.data.remote.ServerConfig
import com.mindaigle.workers.ReminderScheduler

/**
 * Ensures server config and auth are initialized before any Activity,
 * and that the app uses BuildConfig server URL/port (clears any stale debug overrides).
 */
class MindAigleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val ctx = applicationContext
        ServerConfig.init(ctx)
        AuthManager.init(ctx)
        ServerConfig.resetToDefaults(ctx)
        ReminderScheduler.ensureScheduled(ctx)
        // Log uncaught exceptions to help debug app closing
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MindAIgle", "Uncaught exception on ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
