package com.mindaigle.data.health

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/** Local cache for last-synced timestamp. Health data is read from Health Connect; we only cache sync time. */
class HealthDataCache(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun getLastSyncedAt(): Long? = withContext(Dispatchers.IO) {
        val t = prefs.getLong(KEY_LAST_SYNCED, -1L)
        if (t <= 0) null else t
    }

    suspend fun setLastSyncedAt() = withContext(Dispatchers.IO) {
        prefs.edit().putLong(KEY_LAST_SYNCED, System.currentTimeMillis()).apply()
    }

    companion object {
        private const val PREFS_NAME = "health_sync"
        private const val KEY_LAST_SYNCED = "last_synced_at"
    }
}
