package com.example.aifittracker.ui

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aifittracker.db.FitDao
import com.example.aifittracker.db.UserAccount
import com.example.aifittracker.ui.theme.cyberpunkNeonBorder
import java.util.UUID

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    fitDao: FitDao,
    onLoginSuccess: (UserAccount) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRegister by remember { mutableStateOf(false) }

    // Common fields
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Register-only fields
    var name by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06080C), // Deep obsidian black
                        Color(0xFF0B0F19), // Midnight blue
                        Color(0xFF05070A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Header with Glowing Neon Text
            Text(
                text = "NEO-FIT",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E5FF),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "AI-POWERED FITNESS PORTAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF007F),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            // Main Card with Neon Border
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberpunkNeonBorder(
                        colors = listOf(Color(0xFF00E5FF), Color(0xFFFF007F), Color(0xFF9D4EDD), Color(0xFF00E5FF)),
                        borderWidth = 2.dp,
                        shape = RoundedCornerShape(24.dp),
                        glowRadius = 12.dp
                    )
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.85f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Toggle Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (!isRegister) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    isRegister = false
                                    errorMessage = null
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "LOGIN",
                                color = if (!isRegister) Color(0xFF00E5FF) else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isRegister) Color(0xFFFF007F).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    isRegister = true
                                    errorMessage = null
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "REGISTER",
                                color = if (isRegister) Color(0xFFFF007F) else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Input Fields
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color.Gray) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )

                    // Registration specific inputs
                    AnimatedVisibility(
                        visible = isRegister,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Display Name", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFF007F),
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = heightStr,
                                    onValueChange = { heightStr = it },
                                    label = { Text("Height (cm)", color = Color.Gray, fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF007F),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                    )
                                )

                                OutlinedTextField(
                                    value = weightStr,
                                    onValueChange = { weightStr = it },
                                    label = { Text("Weight (kg)", color = Color.Gray, fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF007F),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error,
                            color = Color(0xFFFF007F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button with sweep neon borders
                    val btnColors = if (isRegister) {
                        listOf(Color(0xFFFF007F), Color(0xFF9D4EDD))
                    } else {
                        listOf(Color(0xFF00E5FF), Color(0xFF00838F))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .cyberpunkNeonBorder(
                                colors = btnColors + btnColors.first(),
                                borderWidth = 1.dp,
                                shape = RoundedCornerShape(12.dp),
                                glowRadius = 4.dp
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(btnColors))
                            .clickable {
                                // Reset error
                                errorMessage = null

                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "Username and Password cannot be empty."
                                    return@clickable
                                }

                                if (isRegister) {
                                    if (name.isBlank() || heightStr.isBlank() || weightStr.isBlank()) {
                                        errorMessage = "All registration fields must be completed."
                                        return@clickable
                                    }
                                    val height = heightStr.toDoubleOrNull()
                                    val weight = weightStr.toDoubleOrNull()
                                    if (height == null || height <= 0 || weight == null || weight <= 0) {
                                        errorMessage = "Please enter valid height and weight values."
                                        return@clickable
                                    }

                                    coroutineScope.launch {
                                        // Check username exists
                                        val existingUser = fitDao.getUserAccountByUsername(username.trim())
                                        if (existingUser != null) {
                                            errorMessage = "Username is already registered."
                                        } else {
                                            // Create user
                                            val newUser = UserAccount(
                                                username = username.trim(),
                                                password = password,
                                                name = name.trim(),
                                                height = height,
                                                weight = weight
                                            )
                                            val newId = fitDao.insertUserAccount(newUser)
                                            // Initialize wallet
                                            fitDao.updateCoinBalance(newId.toInt(), 150)

                                            val savedUser = fitDao.getUserAccountById(newId.toInt())
                                            if (savedUser != null) {
                                                Toast.makeText(context, "Registration Successful!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess(savedUser)
                                            } else {
                                                errorMessage = "Database insertion error."
                                            }
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        // Login
                                        val user = fitDao.getUserAccountByUsername(username.trim())
                                        if (user == null || user.password != password) {
                                            errorMessage = "Invalid username or password."
                                        } else {
                                            Toast.makeText(context, "Access Granted. Welcome, ${user.name}!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(user)
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRegister) "REGISTER NOW" else "INITIALIZE SYSTEM",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
