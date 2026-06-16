package com.example.aifittracker.ui

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aifittracker.db.FitDao
import com.example.aifittracker.db.UserAccount
import com.example.aifittracker.ui.theme.cyberpunkNeonBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    user: UserAccount,
    fitDao: FitDao,
    onDismiss: () -> Unit,
    onProfileUpdated: (UserAccount) -> Unit,
    currentTheme: String = "CYBERPUNK",
    onThemeChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var heightStr by remember { mutableStateOf(user.height.toString()) }
    var weightStr by remember { mutableStateOf(user.weight.toString()) }

    var isEditing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Current metrics (Double)
    val currentHeight = heightStr.toDoubleOrNull() ?: user.height
    val currentWeight = weightStr.toDoubleOrNull() ?: user.weight

    // Calculate BMI
    val heightM = currentHeight / 100.0
    val bmi = if (heightM > 0) currentWeight / (heightM * heightM) else 0.0

    // BMI categories and properties
    val (bmiCategory, bmiColor, bmiAdvice) = when {
        bmi < 18.5 -> Triple(
            "UNDERWEIGHT",
            Color(0xFF00E5FF), // Neon Cyan
            "Gain Weight Goal: Focus on hypertrophic strength training & a nutrient-rich caloric surplus."
        )
        bmi < 25.0 -> Triple(
            "NORMAL",
            Color(0xFF39FF14), // Neon Green
            "Maintenance Goal: Great job! Stay active and consume balanced nutrition to maintain your stats."
        )
        bmi < 30.0 -> Triple(
            "OVERWEIGHT",
            Color(0xFFFFEA00), // Laser Yellow
            "Weight Loss Goal: Combine regular cardiovascular exercise with a moderate caloric deficit."
        )
        else -> Triple(
            "OBESE",
            Color(0xFFFF007F), // Hot Pink
            "Action Needed Goal: Prioritize safe low-impact workouts, clean eating, and clinical consultation."
        )
    }

    // Target normal weight range (BMI 18.5 to 24.9)
    val minTargetWeight = 18.5 * (heightM * heightM)
    val maxTargetWeight = 24.9 * (heightM * heightM)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .cyberpunkNeonBorder(
                    borderWidth = 2.dp,
                    shape = RoundedCornerShape(24.dp),
                    glowRadius = 12.dp
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF06080C).copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "USER DOSSIER",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Seed & Name Details
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .cyberpunkNeonBorder(
                            colors = listOf(bmiColor, Color(0xFF9D4EDD)),
                            borderWidth = 1.5.dp,
                            shape = RoundedCornerShape(16.dp),
                            glowRadius = 4.dp
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0D1321)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(2).uppercase(),
                        color = bmiColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = user.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user.username} • Goal: ${user.targetGoal}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp))

                if (!isEditing) {
                    // View Mode stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "HEIGHT", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${currentHeight} cm", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "WEIGHT", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${currentWeight} kg", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Edit metrics trigger button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(38.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .clickable { isEditing = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EDIT METRICS",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                } else {
                    // Edit Mode Form
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = { heightStr = it },
                            label = { Text("Height (cm)", color = Color.Gray, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF)
                            )
                        )

                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = { weightStr = it },
                            label = { Text("Weight (kg)", color = Color.Gray, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF)
                            )
                        )
                    }

                    errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error, color = Color(0xFFFF007F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save / Cancel Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                isEditing = false
                                errorMessage = null
                                heightStr = user.height.toString()
                                weightStr = user.weight.toString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCEL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val height = heightStr.toDoubleOrNull()
                                val weight = weightStr.toDoubleOrNull()
                                if (height == null || height <= 0 || weight == null || weight <= 0) {
                                    errorMessage = "Please enter valid numeric metrics."
                                    return@Button
                                }
                                // Update DB
                                val updatedUser = user.copy(height = height, weight = weight)
                                coroutineScope.launch {
                                    fitDao.updateUserAccount(updatedUser)
                                }
                                Toast.makeText(context, "Metrics Updated!", Toast.LENGTH_SHORT).show()
                                onProfileUpdated(updatedUser)
                                isEditing = false
                                errorMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SAVE", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp))

                Text(
                    text = "SELECT APP THEME",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.Start
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        Triple("CYBERPUNK", "Cyan", Color(0xFF00E5FF)),
                        Triple("SUNSET", "Sunset", Color(0xFFFF9100)),
                        Triple("MATRIX", "Matrix", Color(0xFF00FF66)),
                        Triple("VAPORWAVE", "Vapor", Color(0xFF9D4EDD))
                    )
                    themes.forEach { (themeId, label, color) ->
                        val isSelected = currentTheme == themeId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) color else Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onThemeChanged(themeId) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) color else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp))

                // BMI Calculation Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BMI SCORE",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format("%.1f", bmi),
                        color = bmiColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Custom Interactive Cyberpunk BMI Slider Gauge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barHeight = 8.dp.toPx()
                        val barY = (size.height - barHeight) / 2
                        
                        val totalRange = 20.0
                        val w1 = (((18.5 - 15.0) / totalRange) * size.width).toFloat()
                        val w2 = (((25.0 - 18.5) / totalRange) * size.width).toFloat()
                        val w3 = (((30.0 - 25.0) / totalRange) * size.width).toFloat()
                        val w4 = (((35.0 - 30.0) / totalRange) * size.width).toFloat()

                        drawRect(
                            color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                            topLeft = Offset(0f, barY),
                            size = Size(w1, barHeight)
                        )
                        drawRect(
                            color = Color(0xFF39FF14).copy(alpha = 0.7f),
                            topLeft = Offset(w1, barY),
                            size = Size(w2, barHeight)
                        )
                        drawRect(
                            color = Color(0xFFFFEA00).copy(alpha = 0.7f),
                            topLeft = Offset(w1 + w2, barY),
                            size = Size(w3, barHeight)
                        )
                        drawRect(
                            color = Color(0xFFFF007F).copy(alpha = 0.7f),
                            topLeft = Offset(w1 + w2 + w3, barY),
                            size = Size(w4, barHeight)
                        )

                        // Draw pointer indicator for current BMI
                        val clampedBmi = bmi.coerceIn(15.0, 35.0)
                        val pointerX = (((clampedBmi - 15.0) / totalRange) * size.width).toFloat()
                        
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(pointerX, size.height / 2)
                        )
                        drawCircle(
                            color = bmiColor,
                            radius = 4.dp.toPx(),
                            center = Offset(pointerX, size.height / 2)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("15", color = Color.Gray, fontSize = 9.sp)
                    Text("18.5", color = Color.Gray, fontSize = 9.sp)
                    Text("25", color = Color.Gray, fontSize = 9.sp)
                    Text("30", color = Color.Gray, fontSize = 9.sp)
                    Text("35", color = Color.Gray, fontSize = 9.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BMI Category Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(bmiColor.copy(alpha = 0.15f))
                        .border(1.dp, bmiColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = bmiCategory,
                        color = bmiColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Fitness Goals & Advice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "INTERACTIVE HEALTH PROTOCOL",
                            color = Color(0xFF9D4EDD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Text(
                            text = bmiAdvice,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = String.format("Target Healthy Weight Range: %.1f kg - %.1f kg", minTargetWeight, maxTargetWeight),
                            color = Color(0xFF39FF14),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
