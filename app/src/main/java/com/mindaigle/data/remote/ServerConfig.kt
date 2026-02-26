package com.mindaigle.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.mindaigle.BuildConfig

object ServerConfig {
    private const val PREFS_NAME = "server_config"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_SERVER_PORT = "server_port"
    private const val KEY_USE_BUILD_CONFIG = "use_build_config"
    
    // Production defaults from BuildConfig (set in build.gradle.kts); trim for robustness
    private val BUILD_CONFIG_SERVER_URL = BuildConfig.SERVER_URL.trim()
    private val BUILD_CONFIG_SERVER_PORT = BuildConfig.SERVER_PORT.trim()
    val ENABLE_DEBUG_CONFIG = BuildConfig.ENABLE_DEBUG_CONFIG
    
    private var sharedPreferences: SharedPreferences? = null
    
    /**
     * Initialize with application context (call this in Application class or MainActivity)
     */
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get the base server URL (without /api)
     * Uses BuildConfig values by default (production-ready)
     */
    fun getBaseUrl(): String {
        // Check if user has overridden with custom config (only in debug mode)
        val useCustomConfig = sharedPreferences?.getBoolean(KEY_USE_BUILD_CONFIG, false) == false && ENABLE_DEBUG_CONFIG
        
        val url = if (useCustomConfig) {
            (sharedPreferences?.getString(KEY_SERVER_URL, BUILD_CONFIG_SERVER_URL) ?: BUILD_CONFIG_SERVER_URL).trim()
        } else {
            BUILD_CONFIG_SERVER_URL
        }
        
        val port = getPort().trim()
        
        // No trailing slash
        val cleanUrl = url.trimEnd('/')
        
        // If URL is HTTPS (like tunnel URLs), don't add port
        // If port is specified and URL doesn't already have a port, add it
        // Skip port for HTTPS on standard ports (443) or HTTP on standard ports (80)
        val isHttps = cleanUrl.startsWith("https://")
        val isStandardPort = (isHttps && port == "443") || 
                            (cleanUrl.startsWith("http://") && port == "80")
        
        // Tunnel URLs (HTTPS) don't need port
        if (isHttps) {
            return cleanUrl
        }
        
        return if (port.isNotEmpty() && !isStandardPort && !cleanUrl.contains(":$port")) {
            "$cleanUrl:$port"
        } else {
            cleanUrl
        }
    }
    
    /**
     * Get the API base URL (with /api)
     */
    fun getApiBaseUrl(): String {
        val baseUrl = getBaseUrl()
        return if (baseUrl.endsWith("/api")) {
            baseUrl
        } else {
            "$baseUrl/api"
        }
    }
    
    /**
     * Get the WebSocket URL
     */
    fun getWebSocketUrl(): String {
        return getBaseUrl()
    }
    
    /**
     * Get the server port
     * Uses BuildConfig values by default (production-ready)
     */
    fun getPort(): String {
        val useCustomConfig = sharedPreferences?.getBoolean(KEY_USE_BUILD_CONFIG, false) == false && ENABLE_DEBUG_CONFIG
        
        return if (useCustomConfig) {
            sharedPreferences?.getString(KEY_SERVER_PORT, BUILD_CONFIG_SERVER_PORT) ?: BUILD_CONFIG_SERVER_PORT
        } else {
            BUILD_CONFIG_SERVER_PORT
        }
    }
    
    /**
     * Set the server URL (e.g., "http://192.168.1.100" or "https://api.example.com")
     * Only works in debug builds
     */
    fun setServerUrl(context: Context, url: String) {
        if (!ENABLE_DEBUG_CONFIG) {
            // Production builds use BuildConfig values only
            return
        }
        if (sharedPreferences == null) {
            init(context)
        }
        sharedPreferences?.edit()
            ?.putString(KEY_SERVER_URL, url)
            ?.putBoolean(KEY_USE_BUILD_CONFIG, false)
            ?.apply()
    }
    
    /**
     * Set the server port
     * Only works in debug builds
     */
    fun setPort(context: Context, port: String) {
        if (!ENABLE_DEBUG_CONFIG) {
            // Production builds use BuildConfig values only
            return
        }
        if (sharedPreferences == null) {
            init(context)
        }
        sharedPreferences?.edit()
            ?.putString(KEY_SERVER_PORT, port)
            ?.putBoolean(KEY_USE_BUILD_CONFIG, false)
            ?.apply()
    }
    
    /**
     * Reset to BuildConfig defaults (production values)
     */
    fun resetToDefaults(context: Context) {
        if (sharedPreferences == null) {
            init(context)
        }
        sharedPreferences?.edit()
            ?.putBoolean(KEY_USE_BUILD_CONFIG, true)
            ?.remove(KEY_SERVER_URL)
            ?.remove(KEY_SERVER_PORT)
            ?.apply()
    }
    
    /**
     * Check if using custom config (debug only)
     */
    fun isUsingCustomConfig(): Boolean {
        if (!ENABLE_DEBUG_CONFIG) return false
        return sharedPreferences?.getBoolean(KEY_USE_BUILD_CONFIG, true) == false
    }
    
    /**
     * Get current server configuration for display
     */
    fun getCurrentConfig(): String {
        return "${getBaseUrl()}"
    }
}

