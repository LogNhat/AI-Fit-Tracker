package com.example.aifittracker.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aifittracker.analysis.ExerciseType
import com.example.aifittracker.model.WorkoutRoom
import com.example.aifittracker.net.SocketManager
import com.example.aifittracker.ui.theme.cyberpunkNeonBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RoomWorkoutHUD(
    room: WorkoutRoom,
    onLeaveRoom: (Int) -> Unit
) {
    // Workout states
    var detectedPose by remember { mutableStateOf<com.google.mlkit.vision.pose.Pose?>(null) }
    var imageWidth by remember { mutableStateOf(720) }
    var imageHeight by remember { mutableStateOf(1280) }
    
    var myReps by remember { mutableStateOf(0) }
    var plankDuration by remember { mutableStateOf(0) }
    var squatStateStr by remember { mutableStateOf("STANDING") }
    var feedbackMessage by remember { mutableStateOf("Get Ready") }
    var useFrontCamera by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    // Participant state from WebSocket
    var roomUsers by remember { mutableStateOf(emptyList<Triple<String, Int, String>>()) }

    // Simulated scores/states for fallback
    var minhScore by remember { mutableStateOf(0) }
    var thanhScore by remember { mutableStateOf(0) }
    
    // Auto-update simulated scores/states
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            if (room.exerciseType == ExerciseType.PLANK) {
                minhScore += 1
                thanhScore += if (Math.random() > 0.3) 1 else 0
            } else {
                minhScore += if (Math.random() > 0.4) 1 else 0
                thanhScore += if (Math.random() > 0.5) 1 else 0
            }
        }
    }

    // Plank Timer increment
    LaunchedEffect(squatStateStr) {
        if (room.exerciseType == ExerciseType.PLANK) {
            while (true) {
                delay(1000)
                if (squatStateStr == "CORRECT") {
                    plankDuration++
                }
            }
        }
    }

    // Display counts mapping
    val myDisplayScore = if (room.exerciseType == ExerciseType.PLANK) {
        plankDuration
    } else {
        myReps
    }

    // --- WebSocket Sync ---
    LaunchedEffect(room.id) {
        SocketManager.joinRoom(room.id, "@you_longnhat")
        
        launch {
            SocketManager.roomUpdates.collect { usersJsonStr ->
                val usersList = mutableListOf<Triple<String, Int, String>>()
                try {
                    val array = org.json.JSONArray(usersJsonStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val username = obj.optString("username")
                        val reps = obj.optInt("reps")
                        val state = obj.optString("state")
                        if (username != "@you_longnhat") {
                            usersList.add(Triple(username, reps, state))
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RoomHUD", "Error parsing room users", e)
                }
                roomUsers = usersList
            }
        }
    }

    LaunchedEffect(myDisplayScore, squatStateStr) {
        SocketManager.sendRepUpdate(myDisplayScore, squatStateStr)
    }

    DisposableEffect(room.id) {
        onDispose {
            SocketManager.leaveRoom()
        }
    }

    // Dynamic participants mapping (Real user from WebSocket or Fallback to simulated bots)
    val user1 = roomUsers.getOrNull(0)
    val user2 = roomUsers.getOrNull(1)

    val name1 = user1?.first ?: "Minh Nguyen"
    val scoreVal1 = user1?.second ?: minhScore
    val stateVal1 = user1?.third ?: "Perfect"

    val name2 = user2?.first ?: "Thanh Tran"
    val scoreVal2 = user2?.second ?: thanhScore
    val stateVal2 = user2?.third ?: "Active"

    val myScoreText = if (room.exerciseType == ExerciseType.PLANK) {
        String.format("%02d:%02d", plankDuration / 60, plankDuration % 60)
    } else {
        myReps.toString()
    }

    val minhScoreText = if (room.exerciseType == ExerciseType.PLANK) {
        String.format("%02d:%02d", scoreVal1 / 60, scoreVal1 % 60)
    } else {
        scoreVal1.toString()
    }

    val thanhScoreText = if (room.exerciseType == ExerciseType.PLANK) {
        String.format("%02d:%02d", scoreVal2 / 60, scoreVal2 % 60)
    } else {
        scoreVal2.toString()
    }

    // Calculate In-Room Ranking
    val ranking = remember(myDisplayScore, scoreVal1, scoreVal2, name1, name2) {
        listOf(
            "You" to myDisplayScore,
            name1 to scoreVal1,
            name2 to scoreVal2
        ).sortedByDescending { it.second }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- 2x2 VIDEO STREAM GRID ---
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Row 1
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Screen 1: You (Live Feed)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    CameraPreview(
                        exerciseType = room.exerciseType,
                        isFrontCamera = useFrontCamera,
                        onPoseDetected = { pose, width, height ->
                            detectedPose = pose
                            imageWidth = width
                            imageHeight = height
                        },
                        onRepDetected = {
                            myReps++
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

                    PoseOverlay(
                        pose = detectedPose,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        isFrontCamera = useFrontCamera,
                        squatStateStr = squatStateStr
                    )

                    // Video Label overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "YOU (LIVE)",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Floating User Score Badge
                    Text(
                        text = myScoreText,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Screen 2: Minh Nguyen (Mock stream)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF0D1321))
                        .border(1.dp, Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name1.replace("@", "").take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(name1, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Form: $stateVal1",
                            color = Color(0xFF00E676),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Label overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676)) // green active stream
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = name1.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Score
                    Text(
                        text = minhScoreText,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Row 2
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Screen 3: Thanh Tran (Mock stream)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF0D1321))
                        .border(1.dp, Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1565C0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name2.replace("@", "").take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(name2, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Form: $stateVal2",
                            color = Color(0xFFFFEA00),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Label overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = name2.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Score
                    Text(
                        text = thanhScoreText,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Screen 4: Empty Slot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF06080C))
                        .border(1.dp, Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Invite",
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Mời bạn bè tham gia",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mã phòng: FIT-${room.id.take(4).uppercase()}",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // --- FLOATING IN-ROOM LEADERBOARD (Top Center) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
                .cyberpunkNeonBorder(
                    borderWidth = 1.dp,
                    shape = RoundedCornerShape(16.dp),
                    glowRadius = 6.dp
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF06080C).copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ranking.forEachIndexed { index, pair ->
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD54F) // Gold
                        1 -> Color(0xFFB0BEC5) // Silver
                        else -> Color(0xFFFFAB91) // Bronze
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(rankColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${pair.first}: ${pair.second}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- FLOATING STICKERS ANIMATION LAYER ---
        Box(modifier = Modifier.fillMaxSize()) {
            floatingStickers.forEach { stickerPair ->
                val elapsed = System.currentTimeMillis() - stickerPair.second
                if (elapsed < 3000) {
                    val floatOffset = -200f * (elapsed / 3000f)
                    val alpha = 1f - (elapsed / 3000f)

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 120.dp)
                            .offset(y = floatOffset.dp)
                            .graphicsLayer(alpha = alpha)
                    ) {
                        Text(
                            text = stickerPair.first,
                            fontSize = 48.sp
                        )
                    }
                }
            }
        }

        // --- BOTTOM ROOM ACTION CONTROLS ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .cyberpunkNeonBorder(
                    borderWidth = 1.2.dp,
                    shape = RoundedCornerShape(24.dp),
                    glowRadius = 8.dp
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF06080C).copy(alpha = 0.9f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Control
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Mute audio",
                        tint = Color.White
                    )
                }

                // Switch Camera Control
                IconButton(
                    onClick = { useFrontCamera = !useFrontCamera },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (useFrontCamera) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        tint = if (useFrontCamera) MaterialTheme.colorScheme.onPrimary else Color.White
                    )
                }

                // Sticker Fire Button
                Button(
                    onClick = { floatingStickers.add("🔥" to System.currentTimeMillis()) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔥", fontSize = 20.sp)
                }

                // Sticker Clap Button
                Button(
                    onClick = { floatingStickers.add("👏" to System.currentTimeMillis()) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEA00)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("👏", fontSize = 20.sp)
                }

                // Leave Room Button
                IconButton(
                    onClick = { onLeaveRoom(myDisplayScore) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Leave Room",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
