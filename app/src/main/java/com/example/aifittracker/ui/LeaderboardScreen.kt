package com.example.aifittracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aifittracker.analysis.ExerciseType
import com.example.aifittracker.model.LeaderboardUser
import com.example.aifittracker.net.SocketManager
import com.example.aifittracker.db.FitDatabase
import com.example.aifittracker.ui.theme.cyberpunkNeonBorder
import androidx.compose.ui.platform.LocalContext

@Composable
fun LeaderboardScreen(
    userId: Int,
    username: String,
    displayName: String
) {
    var selectedTab by remember { mutableStateOf(ExerciseType.SQUAT) }
    val context = LocalContext.current
    var globalLeaderboard by remember {
        mutableStateOf(
            listOf(
                LeaderboardUser(1, "plank_master", "2100 pts"),
                LeaderboardUser(2, "fit_queen", "1850 pts"),
                LeaderboardUser(3, "gym_bro", "1420 pts"),
                LeaderboardUser(4, "iron_man", "1200 pts"),
                LeaderboardUser(5, "yoga_mind", "950 pts")
            )
        )
    }

    // Submit score and load leaderboard from server
    LaunchedEffect(Unit) {
        val db = FitDatabase.getDatabase(context)
        val score = db.fitDao().getCoinBalance(userId) ?: 150
        
        val submitName = "@$username"
        SocketManager.submitScore(submitName, score)
        SocketManager.getLeaderboard()
        
        SocketManager.onLeaderboardDataListener = { leaderboardJsonStr ->
            try {
                val array = org.json.JSONArray(leaderboardJsonStr)
                val newList = mutableListOf<LeaderboardUser>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val u = obj.optString("username")
                    val s = obj.optInt("score")
                    val isMe = u == submitName
                    val dispName = if (isMe) "You ($displayName)" else u
                    newList.add(
                        LeaderboardUser(
                            rank = i + 1,
                            name = dispName,
                            score = "$s pts",
                            isCurrentUser = isMe
                        )
                    )
                }
                globalLeaderboard = newList
            } catch (e: Exception) {
                android.util.Log.e("Leaderboard", "Error parsing leaderboard", e)
            }
        }
    }

    val currentList = globalLeaderboard

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
        
        Text(
            text = "LEADERBOARD",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
        
        Text(
            text = "Global Rankings",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Exercise Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .cyberpunkNeonBorder(
                    borderWidth = 1.dp,
                    shape = RoundedCornerShape(20.dp),
                    glowRadius = 4.dp
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0D1321).copy(alpha = 0.8f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ExerciseType.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.displayName.split(" ").first(),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Top 3 Podium
        if (currentList.size >= 3) {
            val first = currentList[0]
            val second = currentList[1]
            val third = currentList[2]

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Rank 2 (Silver)
                PodiumCol(
                    user = second,
                    heightPercentage = 0.8f,
                    borderColor = Color(0xFFB0BEC5),
                    modifier = Modifier.weight(1f)
                )

                // Rank 1 (Gold)
                PodiumCol(
                    user = first,
                    heightPercentage = 1.0f,
                    borderColor = Color(0xFFFFD54F),
                    modifier = Modifier.weight(1.1f),
                    hasCrown = true
                )

                // Rank 3 (Bronze)
                PodiumCol(
                    user = third,
                    heightPercentage = 0.7f,
                    borderColor = Color(0xFFFFAB91),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Remaining List (Rank 4+)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            val remainingList = if (currentList.size > 3) currentList.subList(3, currentList.size) else emptyList()
            items(remainingList) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberpunkNeonBorder(
                            colors = if (user.isCurrentUser) {
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.primary)
                            } else {
                                listOf(Color(0xFF1F2937), Color(0xFF111827))
                            },
                            borderWidth = 1.dp,
                            shape = RoundedCornerShape(20.dp),
                            glowRadius = if (user.isCurrentUser) 6.dp else 0.dp
                        )
                        .background(Color(0xFF0D1321).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Number
                    Text(
                        text = "#${user.rank}",
                        color = if (user.isCurrentUser) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )

                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF232F42)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.first().toString(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Username
                    Text(
                        text = user.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Score
                    Text(
                        text = user.score,
                        color = if (user.isCurrentUser) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun PodiumCol(
    user: LeaderboardUser,
    heightPercentage: Float,
    borderColor: Color,
    modifier: Modifier = Modifier,
    hasCrown: Boolean = false
) {
    val podiumBorderColors = when (user.rank) {
        1 -> listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFFD54F))
        2 -> listOf(Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFFB0BEC5))
        else -> listOf(Color(0xFFFFAB91), Color(0xFFFF8A65), Color(0xFFFFAB91))
    }

    Column(
        modifier = modifier
            .fillMaxHeight(heightPercentage)
            .cyberpunkNeonBorder(
                colors = podiumBorderColors,
                borderWidth = 1.5.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                glowRadius = if (hasCrown) 8.dp else 4.dp
            )
            .background(
                Color(0xFF0D1321).copy(alpha = 0.85f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            if (hasCrown) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Gold Crown",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Text(
                    text = "#${user.rank}",
                    color = borderColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Name
            Text(
                text = user.name.split(" ").first(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // Score Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(borderColor.copy(alpha = 0.15f))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.score,
                color = if (hasCrown) Color(0xFFFFD54F) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

