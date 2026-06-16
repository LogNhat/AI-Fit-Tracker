package com.example.aifittracker.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import com.example.aifittracker.ui.theme.cyberpunkNeonBorder
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aifittracker.analysis.ExerciseType
import com.example.aifittracker.analysis.SquatState
import com.example.aifittracker.db.FitDatabase
import com.example.aifittracker.db.WorkoutLog
import com.example.aifittracker.db.UserAccount
import com.example.aifittracker.ui.LoginScreen
import com.example.aifittracker.ui.ProfileDialog
import com.example.aifittracker.model.WorkoutRoom
import com.example.aifittracker.net.SocketManager
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

data class HUDParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    var size: Float,
    var alpha: Float
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentTheme: String = "CYBERPUNK",
    onThemeChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val particles = remember { mutableListOf<HUDParticle>() }
    var drawTick by remember { mutableStateOf(0L) }


    // Animations for Cyberpunk Rep counter Ripple and Scale
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }
    var scaleTrigger by remember { mutableStateOf(1f) }
    
    val repScale by animateFloatAsState(
        targetValue = scaleTrigger,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rep_scale"
    )

    // WebSocket connection state
    var isSocketConnected by remember { mutableStateOf(SocketManager.isConnected) }
    LaunchedEffect(Unit) {
        SocketManager.onConnectionStateChanged = { connected ->
            isSocketConnected = connected
        }
    }
    
    // Gym Room State
    var activeRoom by remember { mutableStateOf<WorkoutRoom?>(null) }

    // Exercise Selection State
    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }
    
    // Workout State
    var detectedPose by remember { mutableStateOf<com.google.mlkit.vision.pose.Pose?>(null) }
    var imageWidth by remember { mutableStateOf(720) }
    var imageHeight by remember { mutableStateOf(1280) }
    var useFrontCamera by remember { mutableStateOf(true) }
    
    var repCount by remember { mutableStateOf(0) }
    var plankDuration by remember { mutableStateOf(0) }
    var squatStateStr by remember { mutableStateOf("STANDING") }
    var feedbackMessage by remember { mutableStateOf("Ready") }
    var activeLeg by remember { mutableStateOf("None") }
    
    // Database and DAO
    val db = remember { FitDatabase.getDatabase(context) }
    val fitDao = remember { db.fitDao() }
    
    // User Authentication State
    var currentUser by remember { mutableStateOf<UserAccount?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // FitCoin balance and Workout Log Count
    var fitCoinBalance by remember { mutableStateOf(150) }
    var workoutLogsCount by remember { mutableStateOf(0) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var workoutLogs by remember { mutableStateOf(listOf<WorkoutLog>()) }
    var activeWorkoutSubTab by remember { mutableStateOf(0) } // 0 = Exercises, 1 = Analytics

    LaunchedEffect(currentUser, workoutLogsCount) {
        currentUser?.let { user ->
            fitCoinBalance = fitDao.getCoinBalance(user.id) ?: 150
            workoutLogsCount = fitDao.getWorkoutLogsCount(user.id)
            workoutLogs = fitDao.getAllWorkoutLogs(user.id)
        }
    }

    LaunchedEffect(repCount) {
        if (repCount > 0 && selectedExercise != ExerciseType.PLANK) {
            // Generate neon explosion particles
            particles.clear()
            val colors = listOf(Color(0xFF00E5FF), Color(0xFFFF007F), Color(0xFFFFEA00), Color(0xFF00E676))
            for (i in 0..20) {
                val angle = Math.random() * 2 * Math.PI
                val speed = 2f + Math.random() * 6f
                particles.add(
                    HUDParticle(
                        x = 0f,
                        y = 0f,
                        vx = (Math.cos(angle) * speed).toFloat(),
                        vy = (Math.sin(angle) * speed).toFloat(),
                        color = colors.random(),
                        size = 5f + (Math.random() * 8f).toFloat(),
                        alpha = 1f
                    )
                )
            }

            scaleTrigger = 1.3f
            rippleRadius.snapTo(0f)
            rippleAlpha.snapTo(0.8f)
            
            // Animate particles
            val particleJob = launch {
                var step = 0
                while (step < 30) {
                    withFrameMillis { frameTime ->
                        for (i in particles.indices) {
                            val p = particles[i]
                            p.x += p.vx
                            p.y += p.vy
                            p.alpha = kotlin.math.max(0f, p.alpha - 0.03f)
                            p.size = kotlin.math.max(0f, p.size - 0.15f)
                        }
                        drawTick = frameTime
                    }
                    step++
                }
                particles.clear()
                drawTick = System.currentTimeMillis()
            }

            val job1 = launch {
                rippleRadius.animateTo(
                    targetValue = 180f,
                    animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
                )
            }
            val job2 = launch {
                rippleAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
                )
            }
            delay(150)
            scaleTrigger = 1f
            
            particleJob.join()
            job1.join()
            job2.join()
        }
    }

    // Save balance to database when it changes
    LaunchedEffect(fitCoinBalance, currentUser) {
        val user = currentUser
        if (user != null) {
            fitDao.updateCoinBalance(user.id, fitCoinBalance)
        }
    }

    // TTS & Vibration Settings
    var isTtsEnabled by remember { mutableStateOf(true) }
    var isVibrationEnabled by remember { mutableStateOf(true) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Initialize TTS
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // Helper function to speak
    val speak = remember(tts, isTtsEnabled) {
        { text: String ->
            if (isTtsEnabled) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FitTrackerTTS")
            }
        }
    }

    // Plank Timer Logic (Solo Workout)
    LaunchedEffect(squatStateStr, selectedExercise) {
        if (selectedExercise == ExerciseType.PLANK) {
            while (selectedExercise == ExerciseType.PLANK) {
                delay(1000)
                if (squatStateStr == "CORRECT") {
                    plankDuration++
                    // Play a soft vibration/beep every 10 seconds of holding
                    if (plankDuration % 10 == 0) {
                        speak(plankDuration.toString())
                        fitCoinBalance += 10 // Earn 10 FitCoins per 10s of Plank
                        if (isVibrationEnabled) {
                            triggerVibration(context, "short")
                        }
                    }
                }
            }
        } else {
            plankDuration = 0
        }
    }

    // React to Rep Count changes (Squat / Push-up / Jumping Jack / Bicep Curl)
    LaunchedEffect(repCount) {
        if (repCount > 0 && selectedExercise != ExerciseType.PLANK) {
            speak(repCount.toString())
            fitCoinBalance += 5 // Earn 5 FitCoins per rep!
            if (isVibrationEnabled) {
                triggerVibration(context, "success")
            }
        }
    }

    // React to Feedback changes
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage.isNotEmpty() && feedbackMessage != "Ready" && feedbackMessage != "Going down...") {
            speak(feedbackMessage)
            if (feedbackMessage.contains("Too shallow") || feedbackMessage.contains("hips") || feedbackMessage.contains("back") || feedbackMessage.contains("hands") || feedbackMessage.contains("legs")) {
                if (isVibrationEnabled) {
                    triggerVibration(context, "error")
                }
            } else if (feedbackMessage.contains("depth") || feedbackMessage.contains("Perfect") || feedbackMessage.contains("Good") || feedbackMessage.contains("Squeeze")) {
                if (isVibrationEnabled) {
                    triggerVibration(context, "short")
                }
            }
        }
    }

    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val themeTertiary = MaterialTheme.colorScheme.tertiary
    val themeError = MaterialTheme.colorScheme.error

    // Determine state color theme
    val stateColor = when (squatStateStr) {
        "DESCENDING", "FLEXING" -> themePrimary
        "BOTTOM", "CORRECT", "FLEXED", "OPEN" -> themeSecondary
        "ASCENDING", "EXTENDING" -> themeTertiary
        else -> themeError
    }

    if (currentUser == null) {
        LoginScreen(
            fitDao = fitDao,
            onLoginSuccess = { user -> currentUser = user }
        )
    } else if (activeRoom != null) {
        // --- 1. MULTIPLAYER WORKOUT ROOM HUD ---
        RoomWorkoutHUD(
            room = activeRoom!!,
            onLeaveRoom = { score ->
                if (score > 0) {
                    val room = activeRoom
                    if (room != null) {
                        val isPlank = room.exerciseType == ExerciseType.PLANK
                        fitDao.insertWorkoutLog(
                            WorkoutLog(
                                userId = currentUser?.id ?: 0,
                                exerciseType = "${room.exerciseType.displayName} (Multiplayer)",
                                reps = if (isPlank) 0 else score,
                                duration = if (isPlank) score else 0
                            )
                        )
                        workoutLogsCount = fitDao.getWorkoutLogsCount(currentUser?.id ?: 0)
                    }
                }
                activeRoom = null
            }
        )
    } else if (selectedExercise == null) {
        // --- 2. MAIN APP SHELL WITH BOTTOM NAVIGATION ---
        var currentMainTab by remember { mutableStateOf(0) } // 0 = Workouts, 1 = Rooms, 2 = Coach, 3 = Rank, 4 = Store

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0D1321).copy(alpha = 0.9f),
                    modifier = Modifier
                        .height(80.dp)
                        .cyberpunkNeonBorder(
                            borderWidth = 1.5.dp,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            glowRadius = 6.dp
                        )
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    NavigationBarItem(
                        selected = currentMainTab == 0,
                        onClick = { currentMainTab = 0 },
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Workouts") },
                        label = { Text("Workouts", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF06080C),
                            selectedTextColor = themePrimary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = themePrimary
                        )
                    )
                    NavigationBarItem(
                        selected = currentMainTab == 1,
                        onClick = { currentMainTab = 1 },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Rooms") },
                        label = { Text("Rooms", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF06080C),
                            selectedTextColor = themePrimary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = themePrimary
                        )
                    )
                    NavigationBarItem(
                        selected = currentMainTab == 2,
                        onClick = { currentMainTab = 2 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "PT Coach") },
                        label = { Text("Coach", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF06080C),
                            selectedTextColor = themePrimary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = themePrimary
                        )
                    )
                    NavigationBarItem(
                        selected = currentMainTab == 3,
                        onClick = { currentMainTab = 3 },
                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard") },
                        label = { Text("Rank", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF06080C),
                            selectedTextColor = themePrimary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = themePrimary
                        )
                    )
                    NavigationBarItem(
                        selected = currentMainTab == 4,
                        onClick = { currentMainTab = 4 },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Store") },
                        label = { Text("Store", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF06080C),
                            selectedTextColor = themePrimary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = themePrimary
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF06080C),
                                Color(0xFF0B0F19),
                                Color(0xFF05070A)
                            )
                        )
                    )
            ) {
                when (currentMainTab) {
                    0 -> {
                        // Exercise Selection Menu
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                            ) {
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SELECT WORKOUT",
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.5.sp
                                        )
                                        
                                        Text(
                                            text = "AI Posture Detection Coaching",
                                            color = themePrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                                        )
                                    }
                                    
                                    // Cyberpunk Profile Button
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .cyberpunkNeonBorder(
                                                colors = listOf(themePrimary, themeError),
                                                borderWidth = 1.5.dp,
                                                shape = CircleShape,
                                                glowRadius = 4.dp
                                            )
                                            .clip(CircleShape)
                                            .background(Color(0xFF0D1321))
                                            .clickable { showProfileDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                                            color = themePrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Sliding Sub-Tab Selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (activeWorkoutSubTab == 0) themePrimary else Color.Transparent)
                                            .clickable { activeWorkoutSubTab = 0 }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "EXERCISES",
                                            color = if (activeWorkoutSubTab == 0) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (activeWorkoutSubTab == 1) themePrimary else Color.Transparent)
                                            .clickable { activeWorkoutSubTab = 1 }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ANALYTICS",
                                            color = if (activeWorkoutSubTab == 1) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (activeWorkoutSubTab == 0) {
                                    // --- WebSocket Sync Server IP settings ---
                                    var ipText by remember { mutableStateOf(SocketManager.serverIp) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ipText,
                                        onValueChange = { 
                                            ipText = it
                                            SocketManager.saveSettings(context, it, SocketManager.serverPort)
                                        },
                                        label = { Text("Server IP Address", color = Color.Gray, fontSize = 10.sp) },
                                        placeholder = { Text("10.0.2.2") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = themePrimary,
                                            unfocusedBorderColor = Color.Gray
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            SocketManager.disconnect()
                                            SocketManager.connect()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSocketConnected) themeSecondary else themePrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = if (isSocketConnected) "CONNECTED" else "CONNECT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                 // --- SQLite Database Dashboard Card ---
                                 Card(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(bottom = 20.dp)
                                         .cyberpunkNeonBorder(
                                             colors = listOf(themePrimary, themeError, themeTertiary, themePrimary),
                                             borderWidth = 1.dp,
                                             shape = RoundedCornerShape(24.dp),
                                             glowRadius = 4.dp
                                         ),
                                     colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.8f)),
                                     shape = RoundedCornerShape(24.dp)
                                 ) {
                                     Column(
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .padding(16.dp),
                                         verticalArrangement = Arrangement.spacedBy(12.dp)
                                     ) {
                                         // User Profile Row
                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Row(
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.spacedBy(8.dp)
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.Person,
                                                     contentDescription = "User Profile",
                                                     tint = themePrimary,
                                                     modifier = Modifier.size(24.dp)
                                                 )
                                                 Column {
                                                     Text(
                                                         text = "USER PROFILE",
                                                         color = Color.Gray,
                                                         fontSize = 9.sp,
                                                         fontWeight = FontWeight.Bold
                                                     )
                                                     Text(
                                                         text = currentUser?.name ?: "Guest",
                                                         color = Color.White,
                                                         fontSize = 15.sp,
                                                         fontWeight = FontWeight.Black
                                                     )
                                                 }
                                             }
                                             
                                             IconButton(
                                                 onClick = { showProfileDialog = true },
                                                 modifier = Modifier
                                                     .size(36.dp)
                                                     .background(themeError.copy(alpha = 0.15f), CircleShape)
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.Person,
                                                     contentDescription = "Profile Details",
                                                     tint = themeError,
                                                     modifier = Modifier.size(20.dp)
                                                 )
                                             }
                                         }

                                         Divider(color = Color.White.copy(alpha = 0.08f))

                                         // Stats Row
                                         Row(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .clickable { showHistoryDialog = true },
                                             horizontalArrangement = Arrangement.SpaceAround,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             // Left stats: FitCoins balance
                                             Row(
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.spacedBy(8.dp)
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.MonetizationOn,
                                                     contentDescription = "FitCoins",
                                                     tint = Color(0xFFFFD54F),
                                                     modifier = Modifier.size(24.dp)
                                                 )
                                                 Column {
                                                     Text(
                                                         text = "VÍ FITCOIN",
                                                         color = Color.Gray,
                                                         fontSize = 9.sp,
                                                         fontWeight = FontWeight.Bold
                                                     )
                                                     Text(
                                                         text = "$fitCoinBalance xu",
                                                         color = Color.White,
                                                         fontSize = 15.sp,
                                                         fontWeight = FontWeight.Black,
                                                         fontFamily = FontFamily.Monospace
                                                     )
                                                 }
                                             }

                                             // Divider line
                                             Box(
                                                 modifier = Modifier
                                                     .height(30.dp)
                                                     .width(1.dp)
                                                     .background(Color.Gray.copy(alpha = 0.2f))
                                             )

                                             // Right stats: Workout Logs count from SQLite
                                             Row(
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.spacedBy(8.dp)
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.FitnessCenter,
                                                     contentDescription = "Workout Logs",
                                                     tint = themePrimary,
                                                     modifier = Modifier.size(24.dp)
                                                 )
                                                 Column {
                                                     Text(
                                                         text = "LỊCH SỬ SQLITE",
                                                         color = Color.Gray,
                                                         fontSize = 9.sp,
                                                         fontWeight = FontWeight.Bold
                                                     )
                                                     Text(
                                                         text = "$workoutLogsCount bài tập",
                                                         color = Color.White,
                                                         fontSize = 15.sp,
                                                         fontWeight = FontWeight.Black,
                                                         fontFamily = FontFamily.Monospace
                                                     )
                                                 }
                                             }
                                         }
                                     }
                                 }

                                 LazyColumn(
                                     verticalArrangement = Arrangement.spacedBy(16.dp),
                                     modifier = Modifier.fillMaxWidth()
                                 ) {
                                     items(ExerciseType.values()) { exercise ->
                                         val icon = when (exercise) {
                                             ExerciseType.SQUAT -> Icons.Default.Accessibility
                                             ExerciseType.PUSH_UP -> Icons.Default.FitnessCenter
                                             ExerciseType.PLANK -> Icons.Default.HourglassTop
                                             ExerciseType.JUMPING_JACK -> Icons.Default.Accessibility
                                             ExerciseType.BICEP_CURL -> Icons.Default.FitnessCenter
                                         }
                                         
                                         val gradientColors = when (exercise) {
                                             ExerciseType.SQUAT -> listOf(themePrimary, themeSecondary)
                                             ExerciseType.PUSH_UP -> listOf(Color(0xFF00E676), themeTertiary)
                                             ExerciseType.PLANK -> listOf(Color(0xFFFFEA00), themeSecondary)
                                             ExerciseType.JUMPING_JACK -> listOf(themeTertiary, themeError)
                                             ExerciseType.BICEP_CURL -> listOf(themeError, Color(0xFFC62828))
                                         }

                                         Card(
                                             onClick = {
                                                 repCount = 0
                                                 plankDuration = 0
                                                 squatStateStr = "STANDING"
                                                 feedbackMessage = "Ready"
                                                 selectedExercise = exercise
                                             },
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .height(140.dp)
                                                 .cyberpunkNeonBorder(
                                                     colors = gradientColors + listOf(gradientColors.first()),
                                                     borderWidth = 1.dp,
                                                     shape = RoundedCornerShape(24.dp),
                                                     glowRadius = 6.dp
                                                 ),
                                             colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.8f)),
                                             shape = RoundedCornerShape(24.dp)
                                         ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(20.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Exercise Icon
                                                Box(
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(Brush.linearGradient(gradientColors)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = exercise.displayName,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(20.dp))

                                                // Exercise Info
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = exercise.displayName,
                                                        color = Color.White,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = exercise.description,
                                                        color = Color.Gray,
                                                        fontSize = 12.sp,
                                                 lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                } else {
                                    WorkoutDashboard(workoutLogs = workoutLogs)
                                }
                            }
                        }
                    }
                    1 -> {
                        // Rooms Screen
                        RoomsScreen(
                            onRoomJoined = { room ->
                                activeRoom = room
                            }
                        )
                    }
                    2 -> {
                        // PT Coach Screen
                        PTCoachScreen(
                            currentUser = currentUser!!,
                            fitDao = fitDao,
                            onStartWorkout = { exercise ->
                                selectedExercise = exercise
                            }
                        )
                    }
                    3 -> {
                        // Leaderboard Screen
                        LeaderboardScreen(
                            userId = currentUser!!.id,
                            username = currentUser!!.username,
                            displayName = currentUser!!.name
                        )
                    }
                    4 -> {
                        // FitStore Screen
                        FitStoreScreen(
                            userId = currentUser!!.id,
                            fitCoinBalance = fitCoinBalance,
                            onCoinsDeducted = { coins ->
                                fitCoinBalance -= coins
                            },
                            fitDao = fitDao
                        )
                    }
                }
            }
        }
    } else {
        // --- 3. SOLO WORKOUT HUD VIEW ---
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Camera Preview
            CameraPreview(
                exerciseType = selectedExercise!!,
                isFrontCamera = useFrontCamera,
                onPoseDetected = { pose, width, height ->
                    detectedPose = pose
                    imageWidth = width
                    imageHeight = height
                },
                onRepDetected = {
                    repCount++
                },
                onStateChanged = { state ->
                    squatStateStr = state
                },
                onFeedbackChanged = { message ->
                    feedbackMessage = message
                },
                onActiveLegChanged = { leg ->
                    activeLeg = leg
                }
            )

            // Pose Overlay
            PoseOverlay(
                pose = detectedPose,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                isFrontCamera = useFrontCamera,
                squatStateStr = squatStateStr
            )

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (repCount > 0 || plankDuration > 0) {
                                fitDao.insertWorkoutLog(
                                    WorkoutLog(
                                        userId = currentUser?.id ?: 0,
                                        exerciseType = selectedExercise!!.displayName,
                                        reps = repCount,
                                        duration = plankDuration
                                    )
                                )
                                workoutLogsCount = fitDao.getWorkoutLogsCount(currentUser?.id ?: 0)
                            }
                            selectedExercise = null
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to menu",
                            tint = Color.White
                        )
                    }
                    
                    Column {
                        Text(
                            text = selectedExercise!!.displayName.uppercase(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (activeLeg != "None") Color(0xFF00E676) else themeError)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (activeLeg != "None") "Tracking: $activeLeg" else "Searching body...",
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live Coin Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$fitCoinBalance",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = { isTtsEnabled = !isTtsEnabled },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Voice feedback toggle",
                            tint = if (isTtsEnabled) themePrimary else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { isVibrationEnabled = !isVibrationEnabled },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Vibration feedback toggle",
                            tint = if (isVibrationEnabled) themePrimary else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch Camera",
                            tint = if (useFrontCamera) themePrimary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            repCount = 0
                            plankDuration = 0
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset stats",
                            tint = themeError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom Stats Panel
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberpunkNeonBorder(
                            colors = listOf(stateColor, themeTertiary, stateColor),
                            borderWidth = 1.5.dp,
                            shape = RoundedCornerShape(24.dp),
                            glowRadius = 10.dp
                        )
                        .background(Color(0xFF0D1321).copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayCount = when (selectedExercise) {
                        ExerciseType.PLANK -> {
                            val minutes = plankDuration / 60
                            val seconds = plankDuration % 60
                            String.format("%02d:%02d", minutes, seconds)
                        }
                        else -> String.format("%02d", repCount)
                    }
                    val countLabel = when (selectedExercise) {
                        ExerciseType.PLANK -> "DURATION"
                        else -> "REPS"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = countLabel,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (rippleAlpha.value > 0f) {
                                    drawCircle(
                                        color = themePrimary.copy(alpha = rippleAlpha.value),
                                        radius = rippleRadius.value,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                    drawCircle(
                                        color = themeError.copy(alpha = rippleAlpha.value * 0.6f),
                                        radius = rippleRadius.value * 0.7f,
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                }
                                
                                // Draw explosion particles
                                if (drawTick >= 0L) {
                                    particles.forEach { p ->
                                        if (p.alpha > 0f && p.size > 0f) {
                                            drawCircle(
                                                color = p.color.copy(alpha = p.alpha),
                                                radius = p.size,
                                                center = Offset(size.width / 2f + p.x, size.height / 2f + p.y)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            AnimatedContent(
                                targetState = displayCount,
                                transitionSpec = {
                                    slideInVertically { height -> height } + fadeIn() togetherWith
                                            slideOutVertically { height -> -height } + fadeOut()
                                },
                                modifier = Modifier.graphicsLayer(
                                    scaleX = repScale,
                                    scaleY = repScale
                                )
                            ) { targetText ->
                                Text(
                                    text = targetText,
                                    color = Color.White,
                                    fontSize = if (selectedExercise == ExerciseType.PLANK) 28.sp else 42.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(80.dp)
                            .width(1.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                            .padding(horizontal = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .padding(start = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "STATE: $squatStateStr",
                            color = stateColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedContent(
                            targetState = feedbackMessage,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            }
                        ) { targetMessage ->
                            Text(
                                text = targetMessage,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        val past7Days = (0..6).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal
        }.reversed()

        val sdfDay = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dailyReps = past7Days.map { dayCal ->
            val dayStart = dayCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1

            val repsOnDay = workoutLogs.filter { log ->
                log.timestamp in dayStart..dayEnd
            }.sumOf { it.reps }

            val label = sdfDay.format(Date(dayStart))
            label to repsOnDay
        }

        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            modifier = Modifier
                .padding(16.dp)
                .cyberpunkNeonBorder(
                    colors = listOf(themePrimary, themeError, themeTertiary, themePrimary),
                    borderWidth = 1.5.dp,
                    shape = RoundedCornerShape(24.dp),
                    glowRadius = 12.dp
                ),
            containerColor = Color(0xFF06080C),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "LỊCH SỬ TẬP LUYỆN",
                    fontWeight = FontWeight.Black,
                    color = themePrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RepsTrendChart(dailyData = dailyReps)

                    Text(
                        text = "NHẬT KÝ CHI TIẾT",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        if (workoutLogs.isEmpty()) {
                            item {
                                Text(
                                    text = "Chưa có lịch sử tập luyện.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                                )
                            }
                        } else {
                            items(workoutLogs) { log ->
                                val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                                val exerciseName = when (log.exerciseType) {
                                    "SQUAT" -> "Squat (Đùi & Mông)"
                                    "PUSH_UP" -> "Push Up (Hít Đất)"
                                    "PLANK" -> "Plank (Kháng Lực)"
                                    "JUMPING_JACK" -> "Jumping Jack (Nhảy vung tay)"
                                    "BICEP_CURL" -> "Bicep Curl (Cơ tay trước)"
                                    else -> log.exerciseType
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = exerciseName,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = timeStr,
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${log.reps} reps",
                                                color = themePrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "${log.duration}s",
                                                color = Color.LightGray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHistoryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeError, contentColor = Color(0xFF06080C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ĐÓNG", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showProfileDialog) {
        val user = currentUser
        if (user != null) {
            ProfileDialog(
                user = user,
                fitDao = fitDao,
                onDismiss = { showProfileDialog = false },
                onProfileUpdated = { updatedUser ->
                    currentUser = updatedUser
                },
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged
            )
        }
    }
}

// Vibration Helper
@Suppress("DEPRECATION")
private fun triggerVibration(context: Context, type: String) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = when (type) {
            "success" -> VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
            "error" -> VibrationEffect.createWaveform(
                longArrayOf(0, 80, 80, 80), 
                intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE), 
                -1
            )
            "short" -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            else -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        when (type) {
            "success" -> vibrator.vibrate(150)
            "error" -> vibrator.vibrate(longArrayOf(0, 80, 80, 80), -1)
            else -> vibrator.vibrate(50)
        }
    }
}

@Composable
private fun RepsTrendChart(dailyData: List<Pair<String, Int>>) {
    val maxVal = dailyData.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val density = LocalDensity.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val barGradient = remember(primaryColor, errorColor) {
        Brush.verticalGradient(
            colors = listOf(primaryColor, errorColor)
        )
    }

    val textPaintWhite = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    val textPaintGray = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .cyberpunkNeonBorder(
                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary),
                borderWidth = 1.dp,
                shape = RoundedCornerShape(16.dp),
                glowRadius = 4.dp
            )
            .background(Color(0xFF0D1321), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "XU HƯỚNG TẬP LUYỆN (7 NGÀY)",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Canvas(modifier = Modifier.fillMaxSize().weight(1f)) {
            val width = size.width
            val height = size.height
            val numBars = dailyData.size
            val spacing = 20.dp.toPx()
            val totalSpacing = spacing * (numBars - 1)
            val barWidth = (width - totalSpacing) / numBars

            // Draw Grid lines
            val numGridLines = 3
            for (g in 0..numGridLines) {
                val y = height * (g / numGridLines.toFloat())
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw bars
            dailyData.forEachIndexed { index, (label, value) ->
                val x = index * (barWidth + spacing)
                val barHeight = height * (value.toFloat() / maxVal)

                // Cyberpunk neon gradient for bars
                if (value > 0) {
                    drawRoundRect(
                        brush = barGradient,
                        topLeft = androidx.compose.ui.geometry.Offset(x, height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                } else {
                    // Draw a tiny placeholder line for 0 value
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.1f),
                        topLeft = androidx.compose.ui.geometry.Offset(x, height - 4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(barWidth, 4.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // Draw values on top of bars
                if (value > 0) {
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "$value",
                            x + barWidth / 2,
                            height - barHeight - 4.dp.toPx(),
                            textPaintWhite
                        )
                    }
                }

                // Draw date labels below bars
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        label,
                        x + barWidth / 2,
                        height + 12.dp.toPx(),
                        textPaintGray
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun WorkoutDashboard(
    workoutLogs: List<WorkoutLog>
) {
    val themePrimary = MaterialTheme.colorScheme.primary

    // Calculate total reps, total duration, calories
    val totalReps = workoutLogs.sumOf { it.reps }
    val totalDurationSeconds = workoutLogs.sumOf { it.duration }
    val totalCalories = totalReps * 0.4 + (totalDurationSeconds * 0.15) // 0.4 kcal per rep, 0.15 kcal per second of plank
    
    val past7Days = (0..6).map { i ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        cal
    }.reversed()

    val sdfDay = SimpleDateFormat("dd/MM", Locale.getDefault())
    val dailyReps = past7Days.map { dayCal ->
        val dayStart = dayCal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1

        val repsOnDay = workoutLogs.filter { log ->
            log.timestamp in dayStart..dayEnd
        }.sumOf { it.reps }

        val label = sdfDay.format(Date(dayStart))
        label to repsOnDay
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Stats Cards grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calories
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .cyberpunkNeonBorder(
                            colors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100)),
                            borderWidth = 1.dp,
                            shape = RoundedCornerShape(16.dp),
                            glowRadius = 3.dp
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Calories",
                            tint = Color(0xFFFF3D00),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("CALORIES", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f kcal", totalCalories),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Active duration
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .cyberpunkNeonBorder(
                            borderWidth = 1.dp,
                            shape = RoundedCornerShape(16.dp),
                            glowRadius = 3.dp
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Active Time",
                            tint = themePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("ACTIVE TIME", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        val mins = totalDurationSeconds / 60
                        val secs = totalDurationSeconds % 60
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d m", mins, secs),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Reps Trend Chart
        item {
            RepsTrendChart(dailyData = dailyReps)
        }

        // Recent logs title
        item {
            Text(
                text = "RECENT WORKOUTS (SQLite)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Recent logs list
        if (workoutLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Chưa có dữ liệu tập luyện. Hãy bắt đầu tập để tích lũy FitCoins!",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            }
        } else {
            items(workoutLogs.take(5)) { log ->
                val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                val exerciseName = when (log.exerciseType) {
                    "SQUAT" -> "Squat (Đùi & Mông)"
                    "PUSH_UP" -> "Push Up (Hít Đất)"
                    "PLANK" -> "Plank (Kháng Lực)"
                    "JUMPING_JACK" -> "Jumping Jack (Nhảy vung tay)"
                    "BICEP_CURL" -> "Bicep Curl (Cơ tay trước)"
                    else -> log.exerciseType
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(themePrimary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = exerciseName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = timeStr,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${log.reps} reps",
                                color = themePrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            if (log.duration > 0) {
                                Text(
                                    text = "${log.duration}s",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
