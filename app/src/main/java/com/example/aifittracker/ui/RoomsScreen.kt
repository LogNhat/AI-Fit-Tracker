package com.example.aifittracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aifittracker.analysis.ExerciseType
import com.example.aifittracker.model.WorkoutRoom
import com.example.aifittracker.ui.theme.cyberpunkNeonBorder
import java.util.UUID

import com.example.aifittracker.net.SocketManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    onRoomJoined: (WorkoutRoom) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var roomName by remember { mutableStateOf("") }
    var selectedExercise by remember { mutableStateOf(ExerciseType.SQUAT) }
    var maxParticipants by remember { mutableStateOf(4) }

    val roomsList = remember { mutableStateListOf<WorkoutRoom>() }

    LaunchedEffect(Unit) {
        SocketManager.onRoomsListListener = { jsonStr ->
            roomsList.clear()
            try {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id")
                    val name = obj.optString("name")
                    val hostName = obj.optString("hostName")
                    val exerciseTypeName = obj.optString("exerciseType")
                    val exerciseType = try {
                        ExerciseType.valueOf(exerciseTypeName)
                    } catch (e: Exception) {
                        ExerciseType.SQUAT
                    }
                    val participantCount = obj.optInt("participantCount")
                    val maxParticipantsVal = obj.optInt("maxParticipants")
                    roomsList.add(
                        WorkoutRoom(
                            id = id,
                            name = name,
                            hostName = hostName,
                            exerciseType = exerciseType,
                            participantCount = participantCount,
                            maxParticipants = maxParticipantsVal
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("RoomsScreen", "Error parsing rooms list", e)
            }
        }
        SocketManager.getRooms()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06080C),
                        Color(0xFF0B0F19),
                        Color(0xFF05070A)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WORKOUT ROOMS",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Train together in real-time",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Create Room Button
            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Room",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Rooms list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(roomsList) { room ->
                Card(
                    onClick = { onRoomJoined(room) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberpunkNeonBorder(
                            borderWidth = 1.dp,
                            shape = RoundedCornerShape(24.dp),
                            glowRadius = 4.dp
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = room.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Host: ${room.hostName}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }

                            // Members count Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Members",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${room.participantCount}/${room.maxParticipants}",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Exercise tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = room.exerciseType.displayName,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            // Join button
                            Button(
                                onClick = { onRoomJoined(room) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "VÀO PHÒNG",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Room Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            modifier = Modifier
                .padding(16.dp)
                .cyberpunkNeonBorder(
                    borderWidth = 1.5.dp,
                    shape = RoundedCornerShape(24.dp),
                    glowRadius = 12.dp
                ),
            containerColor = Color(0xFF06080C),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "TẠO PHÒNG TẬP",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Room name
                    OutlinedTextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        label = { Text("Tên phòng tập", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    // Selected exercise (Scrollable row)
                    Column {
                        Text(
                            text = "CHỌN BÀI TẬP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            ExerciseType.values().forEach { type ->
                                val isSelected = selectedExercise == type
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF0D1321))
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedExercise = type }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.displayName.split(" ").first(),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Max participants
                    Column {
                        Text(
                            text = "GIỚI HẠN NGƯỜI THAM GIA: $maxParticipants",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                        Slider(
                            value = maxParticipants.toFloat(),
                            onValueChange = { maxParticipants = it.toInt() },
                            valueRange = 2f..8f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.DarkGray
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomName.isNotBlank()) {
                            val roomId = UUID.randomUUID().toString()
                            val hostName = "@you_longnhat"
                            
                            SocketManager.createRoom(
                                roomId = roomId,
                                name = roomName,
                                hostName = hostName,
                                exerciseType = selectedExercise.name,
                                maxParticipants = maxParticipants
                            )
                            SocketManager.joinRoom(roomId, hostName)

                            val newRoom = WorkoutRoom(
                                id = roomId,
                                name = roomName,
                                hostName = hostName,
                                exerciseType = selectedExercise,
                                participantCount = 1,
                                maxParticipants = maxParticipants
                            )
                            onRoomJoined(newRoom)
                        }
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("TẠO MỚI", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("HỦY", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
