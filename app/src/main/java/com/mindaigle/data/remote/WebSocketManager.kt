package com.mindaigle.data.remote

import com.mindaigle.BuildConfig
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

object WebSocketManager {
    private const val TAG = "WebSocketManager"
    
    private var socket: Socket? = null
    private var isConnected = false
    
    // Connection state listeners
    private val connectionListeners = mutableListOf<(Boolean) -> Unit>()
    
    // Event listeners by event type
    private val eventListeners = mutableMapOf<String, MutableList<(Any?) -> Unit>>()
    
    /**
     * Initialize and connect to WebSocket server
     * @param token JWT token for authentication (optional for testing)
     */
    fun connect(token: String? = null) {
        if (socket?.connected() == true) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Already connected")
            return
        }
        
        try {
            val baseUrl = ServerConfig.getWebSocketUrl()
            val options = IO.Options().apply {
                // Add authentication token if provided
                if (token != null) {
                    auth = mapOf("token" to token)
                }
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
            }
            
            socket = IO.socket(baseUrl, options)
            setupEventListeners()
            socket?.connect()
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Connecting to WebSocket server: $baseUrl")
        } catch (e: URISyntaxException) {
            val baseUrl = ServerConfig.getWebSocketUrl()
            Log.e(TAG, "Invalid WebSocket URL: $baseUrl", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to WebSocket", e)
        }
    }
    
    /**
     * Disconnect from WebSocket server
     */
    fun disconnect() {
        socket?.disconnect()
        socket = null
        isConnected = false
        if (BuildConfig.DEBUG) Log.d(TAG, "Disconnected from WebSocket server")
    }
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean = socket?.connected() == true
    
    /**
     * Subscribe to a channel/room
     */
    fun subscribe(channels: List<String>) {
        if (isConnected()) {
            socket?.emit("subscribe", channels)
            if (BuildConfig.DEBUG) Log.d(TAG, "Subscribed to channels: $channels")
        } else {
            Log.w(TAG, "Cannot subscribe: not connected")
        }
    }
    
    /**
     * Unsubscribe from a channel/room
     */
    fun unsubscribe(channels: List<String>) {
        if (isConnected()) {
            socket?.emit("unsubscribe", channels)
            if (BuildConfig.DEBUG) Log.d(TAG, "Unsubscribed from channels: $channels")
        }
    }
    
    /**
     * Send a test message
     */
    fun sendTest(message: String) {
        if (isConnected()) {
            socket?.emit("test", message)
            if (BuildConfig.DEBUG) Log.d(TAG, "Sent test message: $message")
        } else {
            Log.w(TAG, "Cannot send test: not connected")
        }
    }
    
    /**
     * Add listener for connection state changes
     */
    fun addConnectionListener(listener: (Boolean) -> Unit) {
        connectionListeners.add(listener)
    }
    
    /**
     * Remove connection listener
     */
    fun removeConnectionListener(listener: (Boolean) -> Unit) {
        connectionListeners.remove(listener)
    }
    
    /**
     * Add listener for a specific event
     */
    fun addEventListener(event: String, listener: (Any?) -> Unit) {
        if (!eventListeners.containsKey(event)) {
            eventListeners[event] = mutableListOf()
            // Register with socket (will work for both connected and future connections)
            socket?.on(event) { args ->
                val data = args.firstOrNull()
                eventListeners[event]?.forEach { it(data) }
            }
        } else {
            eventListeners[event]?.add(listener)
        }
    }
    
    /**
     * Remove event listener
     */
    fun removeEventListener(event: String, listener: (Any?) -> Unit) {
        eventListeners[event]?.remove(listener)
        if (eventListeners[event]?.isEmpty() == true) {
            eventListeners.remove(event)
            socket?.off(event)
        }
    }
    
    /**
     * Setup default event listeners
     */
    private fun setupEventListeners() {
        socket?.apply {
            // Connection events
            on(Socket.EVENT_CONNECT) {
                isConnected = true
                if (BuildConfig.DEBUG) Log.d(TAG, "WebSocket connected")
                connectionListeners.forEach { it(true) }
            }
            
            on(Socket.EVENT_DISCONNECT) {
                isConnected = false
                if (BuildConfig.DEBUG) Log.d(TAG, "WebSocket disconnected")
                connectionListeners.forEach { it(false) }
            }
            
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                isConnected = false
                Log.e(TAG, "WebSocket connection error: ${args.firstOrNull()}")
                connectionListeners.forEach { it(false) }
            }
            
            // Server confirmation
            on("connected") { args ->
                val data = args.firstOrNull() as? JSONObject
                if (BuildConfig.DEBUG) Log.d(TAG, "Server confirmed connection: ${data?.toString()}")
            }
            
            // Test response
            on("test_response") { args ->
                val data = args.firstOrNull() as? JSONObject
                if (BuildConfig.DEBUG) Log.d(TAG, "Test response received: ${data?.toString()}")
                eventListeners["test_response"]?.forEach { it(data) }
            }
            
            // Real-time alert events
            on("new_alert") { args ->
                val data = args.firstOrNull()
                if (BuildConfig.DEBUG) Log.d(TAG, "New alert received: $data")
                eventListeners["new_alert"]?.forEach { it(data) }
            }
            
            on("alert_created") { args ->
                val data = args.firstOrNull()
                if (BuildConfig.DEBUG) Log.d(TAG, "Alert created confirmation: $data")
                eventListeners["alert_created"]?.forEach { it(data) }
            }
            
            // After connection, re-register any custom event listeners that were added before connection
            on(Socket.EVENT_CONNECT) {
                eventListeners.forEach { (event, _) ->
                    if (event != Socket.EVENT_CONNECT && 
                        event != Socket.EVENT_DISCONNECT && 
                        event != Socket.EVENT_CONNECT_ERROR &&
                        event != "connected" &&
                        event != "test_response" &&
                        event != "new_alert" &&
                        event != "alert_created") {
                        // Re-register custom event listeners
                        on(event) { args ->
                            val data = args.firstOrNull()
                            eventListeners[event]?.forEach { it(data) }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Update authentication token and reconnect
     */
    fun updateToken(token: String) {
        disconnect()
        connect(token)
    }
}

