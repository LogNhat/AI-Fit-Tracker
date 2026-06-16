package com.example.aifittracker.net

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

object SocketManager {
    private const val TAG = "SocketManager"
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Default server IP. Can be modified by user settings in app.
    var serverIp: String = "10.0.2.2" // Emulator default host
    var serverPort: Int = 8080

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("fit_tracker_prefs", Context.MODE_PRIVATE)
        serverIp = prefs.getString("server_ip", "10.0.2.2") ?: "10.0.2.2"
        serverPort = prefs.getInt("server_port", 8080)
        Log.d(TAG, "Settings loaded: IP=$serverIp, Port=$serverPort")
    }

    fun saveSettings(context: Context, ip: String, port: Int) {
        serverIp = ip
        serverPort = port
        val prefs = context.getSharedPreferences("fit_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("server_ip", ip).putInt("server_port", port).apply()
        Log.d(TAG, "Settings saved: IP=$serverIp, Port=$serverPort")
    }
    
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    var isConnected = false
        private set
    
    private var isReconnecting = false
    
    // Callbacks
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onRoomUpdateListener: ((String) -> Unit)? = null // JSON string of users list
    var onChatReceiveListener: ((String, String, Long) -> Unit)? = null // sender, text, timestamp
    var onLeaderboardDataListener: ((String) -> Unit)? = null // JSON array of leaderboard users
    var onRoomsListListener: ((String) -> Unit)? = null // JSON array of rooms list
    
    fun connect() {
        if (isConnected) return
        
        val url = when {
            serverIp.startsWith("ws://") || serverIp.startsWith("wss://") -> {
                serverIp
            }
            serverIp.contains("onrender.com") || serverIp.contains("railway.app") || (!serverIp.contains(":") && serverIp.any { it.isLetter() }) -> {
                "wss://$serverIp"
            }
            else -> {
                "ws://$serverIp:$serverPort"
            }
        }
        Log.d(TAG, "Connecting to WebSocket: $url")
        
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully.")
                isConnected = true
                mainHandler.post {
                    onConnectionStateChanged?.invoke(true)
                }
                
                // Fetch leaderboard right after connection
                getLeaderboard()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received WebSocket message: $text")
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "room_update" -> {
                            val usersArray = json.optJSONArray("users")?.toString() ?: "[]"
                            mainHandler.post {
                                onRoomUpdateListener?.invoke(usersArray)
                            }
                        }
                        "chat_receive" -> {
                            val sender = json.optString("sender")
                            val messageText = json.optString("messageText")
                            val timestamp = json.optLong("timestamp")
                            mainHandler.post {
                                onChatReceiveListener?.invoke(sender, messageText, timestamp)
                            }
                        }
                        "leaderboard_data" -> {
                            val dataArray = json.optJSONArray("data")?.toString() ?: "[]"
                            mainHandler.post {
                                onLeaderboardDataListener?.invoke(dataArray)
                            }
                        }
                        "rooms_list" -> {
                            val dataArray = json.optJSONArray("data")?.toString() ?: "[]"
                            mainHandler.post {
                                onRoomsListListener?.invoke(dataArray)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing websocket message", e)
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                isConnected = false
                mainHandler.post {
                    onConnectionStateChanged?.invoke(false)
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed.")
                isConnected = false
                mainHandler.post {
                    onConnectionStateChanged?.invoke(false)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                isConnected = false
                mainHandler.post {
                    onConnectionStateChanged?.invoke(false)
                }
                
                // Auto reconnect after 5s
                if (!isReconnecting) {
                    isReconnecting = true
                    Thread {
                        try {
                            Thread.sleep(5000)
                            isReconnecting = false
                            if (!isConnected) {
                                connect()
                            }
                        } catch (e: Exception) {
                            isReconnecting = false
                            Log.e(TAG, "Reconnect thread error", e)
                        }
                    }.start()
                }
            }
        })
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Goodbye")
        isConnected = false
    }
    
    fun joinRoom(roomId: String, username: String) {
        val payload = JSONObject().apply {
            put("action", "join_room")
            put("roomId", roomId)
            put("username", username)
        }
        send(payload.toString())
    }
    
    fun sendRepUpdate(reps: Int, state: String) {
        val payload = JSONObject().apply {
            put("action", "rep_update")
            put("reps", reps)
            put("state", state)
        }
        send(payload.toString())
    }
    
    fun leaveRoom() {
        val payload = JSONObject().apply {
            put("action", "leave_room")
        }
        send(payload.toString())
    }
    
    fun sendChat(sender: String, messageText: String) {
        val payload = JSONObject().apply {
            put("action", "send_chat")
            put("sender", sender)
            put("messageText", messageText)
            put("timestamp", System.currentTimeMillis())
        }
        send(payload.toString())
    }
    
    fun getLeaderboard() {
        val payload = JSONObject().apply {
            put("action", "get_leaderboard")
        }
        send(payload.toString())
    }
    
    fun submitScore(username: String, score: Int) {
        val payload = JSONObject().apply {
            put("action", "submit_score")
            put("username", username)
            put("score", score)
        }
        send(payload.toString())
    }
    
    fun getRooms() {
        val payload = JSONObject().apply {
            put("action", "get_rooms")
        }
        send(payload.toString())
    }
    
    fun createRoom(roomId: String, name: String, hostName: String, exerciseType: String, maxParticipants: Int) {
        val payload = JSONObject().apply {
            put("action", "create_room")
            put("roomId", roomId)
            put("name", name)
            put("hostName", hostName)
            put("exerciseType", exerciseType)
            put("maxParticipants", maxParticipants)
        }
        send(payload.toString())
    }
    
    private fun send(text: String) {
        if (isConnected) {
            webSocket?.send(text)
        } else {
            Log.w(TAG, "Cannot send, websocket not connected: $text")
        }
    }
}
