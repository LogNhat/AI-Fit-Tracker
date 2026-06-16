package com.example.aifittracker.net

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _connectionState = MutableStateFlow(false)
    val connectionState = _connectionState.asStateFlow()

    val isConnected: Boolean
        get() = _connectionState.value
    
    private val _roomUpdates = MutableSharedFlow<String>(replay = 0)
    val roomUpdates = _roomUpdates.asSharedFlow()

    data class ChatMessageEvent(val sender: String, val messageText: String, val timestamp: Long)

    private val _chatReceive = MutableSharedFlow<ChatMessageEvent>(replay = 0)
    val chatReceive = _chatReceive.asSharedFlow()

    private val _leaderboardData = MutableSharedFlow<String>(replay = 0)
    val leaderboardData = _leaderboardData.asSharedFlow()

    private val _roomsList = MutableSharedFlow<String>(replay = 0)
    val roomsList = _roomsList.asSharedFlow()
    
    private var isReconnecting = false
    
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
                _connectionState.value = true
                
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
                            scope.launch {
                                _roomUpdates.emit(usersArray)
                            }
                        }
                        "chat_receive" -> {
                            val sender = json.optString("sender")
                            val messageText = json.optString("messageText")
                            val timestamp = json.optLong("timestamp")
                            scope.launch {
                                _chatReceive.emit(ChatMessageEvent(sender, messageText, timestamp))
                            }
                        }
                        "leaderboard_data" -> {
                            val dataArray = json.optJSONArray("data")?.toString() ?: "[]"
                            scope.launch {
                                _leaderboardData.emit(dataArray)
                            }
                        }
                        "rooms_list" -> {
                            val dataArray = json.optJSONArray("data")?.toString() ?: "[]"
                            scope.launch {
                                _roomsList.emit(dataArray)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing websocket message", e)
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                _connectionState.value = false
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed.")
                _connectionState.value = false
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                _connectionState.value = false
                
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
        _connectionState.value = false
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
