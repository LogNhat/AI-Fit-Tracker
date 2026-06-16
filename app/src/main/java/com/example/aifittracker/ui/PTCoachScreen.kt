package com.example.aifittracker.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Send
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
import com.example.aifittracker.db.FitDao
import com.example.aifittracker.db.UserChatMessage
import com.example.aifittracker.db.UserAccount
import com.example.aifittracker.ui.theme.cyberpunkNeonBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTCoachScreen(
    currentUser: UserAccount,
    fitDao: FitDao,
    onStartWorkout: (ExerciseType) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val chatMessagesFlow = remember(currentUser.id) { fitDao.getAllChatMessages(currentUser.id) }
    val chatMessages by chatMessagesFlow.collectAsState(initial = emptyList())
    var typedText by remember { mutableStateOf("") }
    var isAiTyping by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("📊 THỂ TRẠNG") }

    // Load Chat History from Room DB
    LaunchedEffect(currentUser.id) {
        val loaded = fitDao.getAllChatMessages(currentUser.id).first()
        if (loaded.isEmpty()) {
            val welcome = UserChatMessage(
                userId = currentUser.id,
                senderName = "FitAI Assistant",
                messageText = "Chào ${currentUser.name}, tôi là Trợ lý AI PT của bạn! Tôi có thể tư vấn chế độ dinh dưỡng, kỹ thuật tập luyện hoặc lên giáo án cá nhân cho bạn. Hôm nay bạn muốn tập trung vào bài tập nào?",
                isCoach = true
            )
            fitDao.insertChatMessage(welcome)
        }
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

        // Header Title
        Column {
            Text(
                text = "FitAI COACH ASSISTANT",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Autonomous 24/7 AI Personal Trainer",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- SECTION 1: AI PT DASHBOARD CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .cyberpunkNeonBorder(
                    borderWidth = 1.5.dp,
                    shape = RoundedCornerShape(24.dp),
                    glowRadius = 6.dp
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.8f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AI Avatar
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9D4EDD).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = "AI Assistant Avatar",
                            tint = Color(0xFF9D4EDD),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // AI Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FitAI Assistant",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "ONLINE • SYSTEM ENGINES ACTIVE",
                            color = Color(0xFF39FF14),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Specialties Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ML Pose Diagnosis", "Workout Plan Generator", "Nutritional Advice").forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // Assignment Card (Clickable to start workout)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF007F).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFFF007F).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable {
                            onStartWorkout(ExerciseType.SQUAT)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Assignment",
                        tint = Color(0xFFFF007F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GIÁO ÁN AI ĐỀ XUẤT (Bấm để tập):",
                            color = Color(0xFFFF007F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Squat Master Challenge • Mục tiêu 15 Reps",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- SECTION 2: LIVE CHAT WINDOW ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 90.dp)
                .cyberpunkNeonBorder(
                    borderWidth = 1.dp,
                    shape = RoundedCornerShape(24.dp),
                    glowRadius = 4.dp
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF06080C).copy(alpha = 0.9f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AI CHAT FEED",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Message Thread
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    reverseLayout = false
                ) {
                    items(chatMessages.size) { index ->
                        val msg = chatMessages[index]
                        ChatBubble(
                            senderName = msg.senderName,
                            message = msg.messageText,
                            isCoach = msg.isCoach
                        )
                    }
                    if (isAiTyping) {
                        item {
                            Text(
                                text = "FitAI Assistant đang soạn tin nhắn...",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // --- CATEGORY SELECTOR ---
                val categories = listOf("📊 THỂ TRẠNG", "🥗 DINH DƯỠNG", "🏋️ GIÁO ÁN", "🧠 KỸ THUẬT")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF9D4EDD).copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF9D4EDD) else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // --- QUICK-REPLY CHIPS ---
                val quickReplies = when (selectedCategory) {
                    "📊 THỂ TRẠNG" -> listOf("Phân tích BMI", "Đo BMR & TDEE", "Đánh giá Tim mạch")
                    "🥗 DINH DƯỠNG" -> listOf("Thực đơn Tăng cơ 7 ngày", "Thực đơn Giảm mỡ 7 ngày", "Chế độ Hydration")
                    "🏋️ GIÁO ÁN" -> listOf("Lịch tập 3 buổi/tuần", "Lịch tập Cardio Giảm mỡ", "Đề xuất giáo án hôm nay")
                    "🧠 KỸ THUẬT" -> listOf("Kỹ thuật Squat chuẩn AI", "Kỹ thuật Plank chuẩn AI", "Tránh chấn thương khi tập")
                    else -> emptyList()
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    items(quickReplies) { text ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0D1321).copy(alpha = 0.8f))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF00E5FF), Color(0xFF9D4EDD))
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (!isAiTyping) {
                                        isAiTyping = true
                                        val textToSend = text
                                        val userMsg = UserChatMessage(userId = currentUser.id, senderName = "You", messageText = textToSend, isCoach = false)
                                        coroutineScope.launch {
                                            fitDao.insertChatMessage(userMsg)
                                            delay(1000)
                                            val aiResponseText = generateLocalAiResponse(textToSend, fitDao, currentUser)
                                            val aiMsg = UserChatMessage(userId = currentUser.id, senderName = "FitAI Assistant", messageText = aiResponseText, isCoach = true)
                                            fitDao.insertChatMessage(aiMsg)
                                            isAiTyping = false
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        placeholder = { Text("Nhập tin nhắn để hỏi AI PT...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                        )
                    )
                    IconButton(
                        onClick = {
                            if (typedText.isNotBlank() && !isAiTyping) {
                                isAiTyping = true
                                val textToSend = typedText
                                typedText = ""
                                
                                // 1. Save and add User message
                                val userMsg = UserChatMessage(
                                    userId = currentUser.id,
                                    senderName = "You",
                                    messageText = textToSend,
                                    isCoach = false
                                )
                                coroutineScope.launch {
                                    fitDao.insertChatMessage(userMsg)
                                    
                                    // 2. Trigger AI response delay
                                    delay(1000)
                                    
                                    // 3. Generate, save and add AI response
                                    val aiResponseText = generateLocalAiResponse(textToSend, fitDao, currentUser)
                                    val aiMsg = UserChatMessage(
                                        userId = currentUser.id,
                                        senderName = "FitAI Assistant",
                                        messageText = aiResponseText,
                                        isCoach = true
                                    )
                                    fitDao.insertChatMessage(aiMsg)
                                    isAiTyping = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Local Smart AI Response Generator based on database context and input keywords
private suspend fun generateLocalAiResponse(userInput: String, fitDao: FitDao, currentUser: UserAccount): String {
    val cleanInput = userInput.lowercase()
    val latestWorkout = fitDao.getAllWorkoutLogs(currentUser.id).first().firstOrNull()
    
    val dateStr = latestWorkout?.let {
        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp))
    }
    val workoutStatus = if (latestWorkout != null) {
        "Dựa trên lịch sử SQLite: Gần đây nhất bạn đã hoàn thành bài tập **${latestWorkout.exerciseType}** với **${latestWorkout.reps} reps** (${latestWorkout.duration} giây) vào lúc **$dateStr**. "
    } else {
        "Tôi chưa ghi nhận lịch sử buổi tập nào gần đây của bạn trong SQLite. Hãy bắt đầu một buổi tập đầu tiên để AI theo dõi và phân tích nhé! "
    }

    val weight = currentUser.weight
    val heightCm = currentUser.height
    val heightM = heightCm / 100.0
    val age = currentUser.age

    return when {
        cleanInput.contains("bmi") || cleanInput.contains("thể trạng") || cleanInput.contains("phân tích bmi") -> {
            val bmi = weight / (heightM * heightM)
            val category = when {
                bmi < 18.5 -> "Thiếu cân (Underweight)"
                bmi < 25.0 -> "Bình thường (Normal Weight)"
                bmi < 30.0 -> "Thừa cân (Overweight)"
                else -> "Béo phì (Obese)"
            }
            val minHealthyWeight = 18.5 * heightM * heightM
            val maxHealthyWeight = 24.9 * heightM * heightM
            
            // TDEE: Mifflin-St Jeor Formula
            val bmr = 10.0 * weight + 6.25 * heightCm - 5.0 * age + 5.0
            val tdee = bmr * 1.375 // Lightly active
            
            """
            🤖 **PHÂN TÍCH CHỈ SỐ BMI & THỂ TRẠNG** 🤖
            
            * Xin chào **${currentUser.name}**, dưới đây là chẩn đoán thể trạng tự động của bạn:
            
            1️⃣ **Chỉ số BMI**: **${"%.2f".format(bmi)}**
            2️⃣ **Phân loại**: **$category**
            3️⃣ **Cân nặng hợp lý mục tiêu**: **${"%.1f".format(minHealthyWeight)} kg - ${"%.1f".format(maxHealthyWeight)} kg** (Hiện tại: **${"%.1f".format(weight)} kg** ở chiều cao **${heightCm.toInt()} cm**)
            4️⃣ **Nhu cầu năng lượng hàng ngày (TDEE)**: **~${tdee.toInt()} kcal/ngày**
            
            ⚡️ *Nhận xét*: 
            ${if (bmi < 18.5) "Thể trạng của bạn đang dưới mức cân đối. Bạn nên tập trung vào chế độ thặng dư calo (nạp nhiều calo hơn TDEE) và tăng cường bài tập kháng lực." 
              else if (bmi < 25.0) "Thể trạng của bạn rất cân đối và khỏe mạnh! Hãy tiếp tục duy trì chế độ ăn uống hiện tại và tập luyện đều đặn." 
              else "Bạn đang có xu hướng thừa cân. Đề xuất thâm hụt calo nhẹ (nạp ít hơn TDEE khoảng 300-500 kcal) kết hợp với các bài tập kháng lực và Cardio để giảm mỡ hiệu quả."}
            
            ---
            📊 *Lịch sử luyện tập*:
            $workoutStatus
            """.trimIndent()
        }

        cleanInput.contains("bmr") || cleanInput.contains("tdee") -> {
            val bmr = 10.0 * weight + 6.25 * heightCm - 5.0 * age + 5.0
            val tdeeSedentary = bmr * 1.2
            val tdeeLightly = bmr * 1.375
            val tdeeModerately = bmr * 1.55
            val tdeeVeryActive = bmr * 1.725
            
            val targetCalories = if (currentUser.targetGoal == "Tăng cơ") tdeeLightly + 300 else tdeeLightly - 400

            """
            📊 **PHÂN TÍCH CHỈ SỐ BMR & TDEE TỰ ĐỘNG** 📊
            
            Chỉ số năng lượng cá nhân của **${currentUser.name}**:
            
            🔥 **1. Tỷ lệ trao đổi chất cơ bản (BMR)**: **~${bmr.toInt()} kcal/ngày**
               *Đây là mức năng lượng tối thiểu cơ thể cần để duy trì sự sống (khi nằm yên không hoạt động).*
               
            ⚡ **2. Nhu cầu tiêu thụ năng lượng hàng ngày (TDEE)**:
               * 🚫 *Ít vận động (Chỉ làm việc văn phòng)*: **~${tdeeSedentary.toInt()} kcal**
               * 🏃 *Vận động nhẹ (Tập thể thao 1-3 buổi/tuần)*: **~${tdeeLightly.toInt()} kcal**
               * 🚴 *Vận động vừa (Tập thể thao 3-5 buổi/tuần)*: **~${tdeeModerately.toInt()} kcal**
               * 🏋️ *Vận động nặng (Tập cường độ cao 6-7 buổi/tuần)*: **~${tdeeVeryActive.toInt()} kcal**
               
            🎯 **3. Đề xuất lượng Calo nạp vào hàng ngày**:
               * Mục tiêu hiện tại của bạn: **${currentUser.targetGoal}**
               * Bạn nên nạp khoảng: **~${targetCalories.toInt()} kcal/ngày** để tối ưu hóa tiến trình.
               
            ---
            💪 *Trạng thái tập luyện*:
            $workoutStatus
            """.trimIndent()
        }

        cleanInput.contains("tim mạch") || cleanInput.contains("đánh giá tim mạch") -> {
            val maxHr = 220 - age
            val z1Min = (maxHr * 0.50).toInt()
            val z1Max = (maxHr * 0.60).toInt()
            val z2Min = (maxHr * 0.60).toInt()
            val z2Max = (maxHr * 0.70).toInt()
            val z3Min = (maxHr * 0.70).toInt()
            val z3Max = (maxHr * 0.80).toInt()
            val z4Min = (maxHr * 0.80).toInt()
            val z4Max = (maxHr * 0.90).toInt()
            val z5Min = (maxHr * 0.90).toInt()
            val z5Max = maxHr

            """
            💓 **ĐÁNH GIÁ TIM MẠCH & PHÂN CHIA VÙNG NHỊP TIM (HEART RATE ZONES)** 💓
            
            Dành cho **${currentUser.name}** (${age} tuổi). Nhịp tim tối đa lý thuyết: **$maxHr bpm** (nhịp/phút).
            
            🟢 **Zone 1: Khởi động & Phục hồi (50-60% Max HR)**: **$z1Min - $z1Max bpm**
               *Lợi ích*: Làm nóng cơ thể, tăng cường tuần hoàn máu, hỗ trợ phục hồi cơ bắp.
               
            🔵 **Zone 2: Đốt mỡ thừa & Sức bền (60-70% Max HR)**: **$z2Min - $z2Max bpm**
               *Lợi ích*: Đây là **vùng vàng đốt mỡ**. Cơ thể ưu tiên sử dụng chất béo làm năng lượng chính.
               
            🟡 **Zone 3: Aerobic / Nâng cao thể lực (70-80% Max HR)**: **$z3Min - $z3Max bpm**
               *Lợi ích*: Cải thiện dung tích phổi, tăng sức mạnh cơ tim và hệ thống tim mạch.
               
            🟠 **Zone 4: Anaerobic / Tăng ngưỡng Acid Lactic (80-90% Max HR)**: **$z4Min - $z4Max bpm**
               *Lợi ích*: Tăng sức chịu đựng của cơ bắp, hỗ trợ tăng cơ và sức bền kỵ khí.
               
            🔴 **Zone 5: Đỉnh giới hạn (90-100% Max HR)**: **$z5Min - $z5Max bpm**
               *Lưu ý*: Chỉ tập luyện ngắn hạn, dùng trong bứt tốc HIIT cường độ cực cao.
               
            💡 Đề xuất: Khi tập các bài Squat hay Plank trên ứng dụng, hãy cố gắng giữ nhịp tim ổn định ở vùng **Zone 2 và Zone 3** để tối đa hóa hiệu quả đốt mỡ và bảo vệ tim mạch tốt nhất!
            """.trimIndent()
        }

        cleanInput.contains("tăng cơ 7 ngày") -> {
            val bmr = 10.0 * weight + 6.25 * heightCm - 5.0 * age + 5.0
            val tdee = bmr * 1.375
            val targetCalories = tdee + 300
            val proteinGrams = weight * 2.0
            
            """
            🍗 **THỰC ĐƠN TĂNG CƠ TỐI ƯU 7 NGÀY (HYPERTROPHY MEAL PLAN)** 🍗
            
            Mục tiêu nạp năng lượng đề xuất: **~${targetCalories.toInt()} kcal/ngày** với **~${proteinGrams.toInt()}g Protein/ngày**.
            
            📅 **Lịch ăn tham khảo hàng ngày**:
            * **Sáng (07:00)**: 70g yến mạch nấu chín + 1 quả chuối + 1 muỗng Whey Protein (hoặc 3 lòng trắng trứng + 2 lát bánh mì đen).
            * **Bữa phụ 1 (10:00)**: 1 hộp sữa chua Hy Lạp + 30g hạt hạnh nhân hoặc óc chó.
            * **Trưa (12:30)**: 180g ức gà áp chảo (hoặc bò nạc) + 150g cơm lứt + rau cải xanh bông cải luộc.
            * **Bữa phụ trước tập (16:00)**: 1 quả táo + 2 lát bánh mì nguyên cám phết bơ đậu phộng.
            * **Tối (19:30 - Sau tập)**: 180g phi-lê cá hồi (hoặc tôm) + 150g khoai lang luộc + xà lách trộn dầu olive.
            
            💡 **Nguyên tắc vàng**:
            1. Ăn thặng dư calo nhưng chọn calo sạch (Clean Bulking).
            2. Nạp Protein cách nhau mỗi 3-4 tiếng để tối ưu tổng hợp cơ bắp.
            3. Uống đủ nước để vận chuyển dinh dưỡng vào cơ.
            """.trimIndent()
        }

        cleanInput.contains("giảm mỡ 7 ngày") -> {
            val bmr = 10.0 * weight + 6.25 * heightCm - 5.0 * age + 5.0
            val tdee = bmr * 1.375
            val targetCalories = tdee - 400
            val proteinGrams = weight * 1.8
            
            """
            🥗 **THỰC ĐƠN GIẢM MỠ & SIẾT CƠ 7 NGÀY (FAT LOSS PROTOCOL)** 🥗
            
            Mục tiêu thâm hụt calo đề xuất: **~${targetCalories.toInt()} kcal/ngày** với **~${proteinGrams.toInt()}g Protein/ngày**.
            
            📅 **Lịch ăn kiểm soát calo**:
            * **Sáng (07:00)**: 2 quả trứng luộc + 1 lát bánh mì đen + 1 ly cà phê đen không đường (hỗ trợ đốt mỡ).
            * **Bữa phụ 1 (10:00)**: 1 quả ổi hoặc 1 quả táo xanh (giàu chất xơ, ít đường).
            * **Trưa (12:30)**: 150g ức gà hấp/áp chảo + 100g cơm gạo lứt + 200g rau luộc (bông cải, bắp cải).
            * **Bữa phụ trước tập (16:00)**: 1 ly sữa đậu nành không đường hoặc Whey pha nước lọc.
            * **Tối (19:00)**: 150g cá tuyết hoặc tôm nướng + 1 chén canh rau ngót thịt bằm + salad dưa leo cà chua xốt chanh.
            
            💡 **Nguyên tắc giảm mỡ**:
            1. Hạn chế tối đa tinh bột nhanh (trắng), đường, nước ngọt, đồ chiên rán dầu mỡ.
            2. Tăng cường chất xơ để tạo cảm giác no lâu, tránh thèm ăn vặt.
            3. Đảm bảo lượng đạm cao để giữ khối lượng cơ nạc trong khi giảm mỡ.
            """.trimIndent()
        }

        cleanInput.contains("hydration") || cleanInput.contains("nước") -> {
            val baseWater = weight * 0.033
            
            """
            💧 **CHẾ ĐỘ HYDRATION - HƯỚNG DẪN BỔ SUNG NƯỚC KHOA HỌC** 💧
            
            Nước chiếm 70% cơ bắp. Thiếu nước 2% sẽ làm giảm hiệu suất tập luyện đến 20%!
            
            🥤 **Lượng nước uống khuyến nghị cho ${currentUser.name}**:
            * Mức cơ bản hàng ngày (không tập): **~${String.format("%.2f", baseWater)} Lít**.
            * Ngày có tập luyện: Cộng thêm **500ml - 700ml** nước cho mỗi 45 phút tập.
            
            📅 **Thời điểm uống nước tối ưu**:
            1️⃣ **Ngay sau khi thức dậy**: Uống 300ml nước ấm để kích hoạt hệ tiêu hóa và bù nước sau đêm dài.
            2️⃣ **Trước khi tập (1-2 tiếng)**: Uống 400ml - 500ml để các tế bào cơ bắp được ngậm đủ nước.
            3️⃣ **Trong lúc tập**: Uống từng ngụm nhỏ (khoảng 100-150ml) mỗi 15 phút. Không uống ngụm lớn gây xóc hông.
            4️⃣ **Sau khi tập**: Uống 500ml để bù đắp lượng mồ hôi đã mất đi.
            
            ⚡️ *Mẹo*: Nếu buổi tập kéo dài và mồ hôi ra nhiều, hãy bổ sung nước điện giải (Electrolytes) chứa Natri, Kali để tránh chuột rút cơ bắp.
            """.trimIndent()
        }

        cleanInput.contains("3 buổi/tuần") || cleanInput.contains("3 buổi") -> {
            """
            🏋️ **LỊCH TẬP 3 BUỔI/TUẦN CHO NGƯỜI MỚI (3-DAY FULL BODY SPLIT)** 🏋️
            
            Lịch tập này tối ưu tần suất phục hồi, cực kỳ phù hợp cho người bận rộn nhưng vẫn đạt hiệu quả cao.
            
            📅 **Chi tiết lịch tập**:
            * **Thứ 2: Buổi 1 - Thân Dưới & Bụng (Lower Focus)**
               * Warm-up: 5 phút xoay khớp cổ chân, hông.
               * Squat Master (AI Camera): 4 sets x 12 reps.
               * Lunge không tạ: 3 sets x 10 reps mỗi bên.
               * Plank Hold (AI Camera): 3 sets x 40s.
            * **Thứ 4: Buổi 2 - Thân Trên (Upper Focus)**
               * Warm-up: Xoay khớp vai, cổ tay.
               * Push-ups (Chống đẩy): 4 sets x 10-12 reps.
               * Bicep Curl (Gập tay trước): 3 sets x 12 reps.
               * Triceps Dip (Nhún tay sau): 3 sets x 10 reps.
            * **Thứ 6: Buổi 3 - Toàn Thân Cường Độ Cao (Full Body Conditioning)**
               * Warm-up: 3 phút Jumping Jacks.
               * Squat Jump: 3 sets x 10 reps.
               * Plank to Push-up: 3 sets x 30s.
               * Mountain Climber: 3 sets x 40s.
               
            休息 *Lưu ý*: Hãy dành các ngày Thứ 3, Thứ 5, Thứ 7 và Chủ Nhật để cơ bắp nghỉ ngơi và phát triển. Ngủ đủ 7-8 tiếng mỗi đêm!
            """.trimIndent()
        }

        cleanInput.contains("cardio giảm mỡ") || cleanInput.contains("cardio") -> {
            """
            🏃 **GIÁO ÁN LUYỆN TẬP CARDIO GIẢM MỠ HIỆU QUẢ** 🏃
            
            Cardio là phương pháp tốt nhất để rèn luyện hệ tim mạch và đốt cháy lượng mỡ thừa tích tụ.
            
            🔥 **1. Phương án HIIT (Cường độ cao ngắt quãng - Rút ngắn thời gian)**:
               * **Cách thực hiện**: 30 giây Jumping Jacks hết tốc lực, sau đó đi bộ nhẹ nhàng tại chỗ 30 giây. Lặp lại chu kỳ từ 15-20 phút.
               * **Lợi ích**: Đốt calo cực lớn và tiếp tục đốt calo sau khi tập xong (hiệu ứng EPOC).
            
            🚴 **2. Phương án LISS (Cường độ trung bình đều đặn - Sức bền)**:
               * **Cách thực hiện**: Đi bộ nhanh hoặc chạy bộ nhẹ nhàng duy trì nhịp tim ở vùng **Zone 2** (khoảng 60-70% nhịp tim tối đa) liên tục từ 45-60 phút.
               * **Lợi ích**: Dễ thực hiện, phục hồi nhanh, an toàn cho khớp gối.
            """.trimIndent()
        }

        cleanInput.contains("chấn thương") || cleanInput.contains("tránh chấn thương") -> {
            """
            🧠 **NGUYÊN TẮC PHÒNG TRÁNH CHẤN THƯƠNG TRONG LUYỆN TẬP** 🧠
            
            Tập luyện là một chặng đường dài, an toàn phải được đặt lên hàng đầu. Hãy ghi nhớ các lưu ý từ AI PT:
            
            1️⃣ **Khởi động kỹ (Warm-up is Mandatory)**:
               * Dành 5-10 phút xoay khớp toàn thân và thực hiện các động tác giãn cơ động (Dynamic stretching). Khởi động giúp bôi trơn khớp xương và làm ấm cơ bắp.
            2️⃣ **Tập trung vào chất lượng kỹ thuật thay vì số lượng**:
               * Độ chính xác tư thế (được camera AI chấm điểm) cực kỳ quan trọng. Không võng lưng khi Plank, không đổ gối khi Squat.
            3️⃣ **Hít thở đúng cách (Breathing Technique)**:
               * Nguyên tắc chung: Thở ra khi dùng lực (khi đẩy tạ lên/đứng dậy trong Squat) và hít vào khi hạ người xuống/nhả lực. Đừng nín thở lâu gây tăng huyết áp đột ngột.
            4️⃣ **Lắng nghe cơ thể và cho phép cơ bắp phục hồi**:
               * Tránh tập luyện quá sức (Overtraining). Đau mỏi cơ nhẹ (DOMS) là bình thường, nhưng đau nhói nhức khớp xương là dấu hiệu cảnh báo cần dừng tập ngay lập tức!
            5️⃣ **Giãn cơ sau tập (Cool-down)**:
               * Dành 5 phút giãn cơ tĩnh (Static stretching) để cơ bắp trở lại trạng thái thư giãn, đẩy nhanh quá trình đào thải Acid Lactic gây mỏi cơ.
            """.trimIndent()
        }

        cleanInput.contains("dinh dưỡng") || cleanInput.contains("ăn uống") -> {
            val minProtein = weight * 1.6
            val maxProtein = weight * 2.2
            
            val bmr = 10.0 * weight + 6.25 * heightCm - 5.0 * age + 5.0
            val tdee = bmr * 1.375
            
            // Macros: 40% Carb, 30% Protein, 30% Fat
            val carbCalories = tdee * 0.40
            val proteinCalories = tdee * 0.30
            val fatCalories = tdee * 0.30
            
            val carbGrams = carbCalories / 4.0
            val proteinGrams = proteinCalories / 4.0
            val fatGrams = fatCalories / 9.0

            """
            🥗 **TƯ VẤN DINH DƯỠNG & PHÂN BỔ MACROS CHUYÊN NGHIỆP** 🥗
            
            Để tối ưu hóa hiệu quả tập luyện dựa trên cân nặng **${"%.1f".format(weight)} kg** của bạn, đây là kế hoạch phân bổ dinh dưỡng đề xuất:
            
            🍗 **1. Lượng Protein Khuyến Nghị**:
            * Mỗi ngày: **${"%.1f".format(minProtein)}g - ${"%.1f".format(maxProtein)}g** (tương đương 1.6g - 2.2g trên mỗi kg cân nặng).
            
            📊 **2. Tỷ Lệ Macros Tối Ưu (40% Carb / 30% Protein / 30% Fat)**:
            * **Carbohydrate (40%)**: **~${carbGrams.toInt()}g** (~${carbCalories.toInt()} kcal)
            * **Protein (30%)**: **~${proteinGrams.toInt()}g** (~${proteinCalories.toInt()} kcal)
            * **Fat (30%)**: **~${fatGrams.toInt()}g** (~${fatCalories.toInt()} kcal)
            * **Tổng Calo Đề Xuất (TDEE)**: **~${tdee.toInt()} kcal/ngày**
            
            💡 **Lời khuyên từ AI Personal Trainer**:
            * **Carbs**: Lựa chọn Carb phức hợp như gạo lứt, khoai lang, yến mạch để giải phóng năng lượng bền bỉ.
            * **Protein**: Ưu tiên ức gà, trứng, cá hồi, thịt bò nạc và whey protein.
            * **Chất béo**: Chọn chất béo không bão hòa từ quả bơ, các loại hạt và dầu olive.
            
            ---
            💪 *Trạng thái luyện tập gần nhất*:
            $workoutStatus
            """.trimIndent()
        }

        cleanInput.contains("giáo án") || cleanInput.contains("bài tập") || cleanInput.contains("đề xuất giáo án") -> {
            val maxHr = 220 - age
            val targetHr = maxHr * 0.7
            
            """
            📋 **GIÁO ÁN TẬP LUYỆN CÁ NHÂN HÓA HÀNG NGÀY** 📋
            
            Chào **${currentUser.name}** (${currentUser.age} tuổi), đây là chương trình luyện tập tối ưu dựa trên nhịp tim và thể trạng của bạn:
            
            💓 **Thông số Tim mạch Mục tiêu**:
            * **Nhịp tim tối đa (Max Heart Rate)**: **$maxHr bpm**
            * **Vùng đốt mỡ mục tiêu (Fat Burn Zone - 70% Max HR)**: **~${targetHr.toInt()} bpm** (giúp tối ưu hóa tiêu thụ chất béo và tăng cường sức bền).
            
            🏋️ **Chương Trình Tập Luyện Hôm Nay**:
            1️⃣ **Warm-up (Khởi động)**: 5-8 phút xoay khớp cổ tay, cổ chân, vai và Squat không tạ nhẹ nhàng.
            2️⃣ **Main Workout (Bài tập chính)**:
               * **Squat Master Challenge**: 4 sets x 15 reps (Thời gian nghỉ giữa set: 60s). *Chú ý: Hạ thấp mông sao cho đùi song song với mặt đất.*
               * **Plank Core Hold**: 3 sets x 45s (Thời gian nghỉ giữa set: 45s). *Chú ý: Giữ lưng thẳng, siết chặt cơ bụng và thở đều.*
            3️⃣ **Cool-down (Giãn cơ)**: 5 phút giãn cơ đùi trước, đùi sau và lưng dưới để giảm mỏi cơ (DOMS).
            
            ---
            ⚡️ *Lịch sử SQLite*:
            $workoutStatus
            """.trimIndent()
        }

        cleanInput.contains("squat") -> {
            """
            🏋️ **HƯỚNG DẪN KỸ THUẬT SQUAT CHUẨN AI** 🏋️
            
            Squat là bài tập nền tảng phát triển sức mạnh toàn bộ thân dưới (đùi trước, mông, đùi sau). Dưới đây là hướng dẫn kỹ thuật chuẩn để AI nhận diện chính xác:
            
            1️⃣ **Tư thế chuẩn bị (Setup)**:
               * Đứng hai chân rộng bằng vai, mũi bàn chân xoay nhẹ ra ngoài khoảng 15 độ.
               * Lưng giữ thẳng tự nhiên, mắt nhìn thẳng về phía trước.
            2️⃣ **Giai đoạn hạ người (Eccentric)**:
               * Hít sâu vào bằng mũi, đẩy nhẹ hông ra phía sau rồi từ từ hạ mông xuống.
               * Giữ đầu gối hướng theo mũi chân, không được để đầu gối chụm vào nhau (Knee Valgus).
            3️⃣ **Điểm sâu nhất (Depth)**:
               * Hạ mông xuống cho đến khi đùi song song hoặc thấp hơn mặt đất (khớp gối đạt góc dưới 95 độ). ML Camera sẽ nhận diện mốc này để tính rep hợp lệ!
            4️⃣ **Giai đoạn đẩy lên (Concentric)**:
               * Ấn gót chân xuống đất, dùng cơ đùi và cơ mông phát lực đẩy cơ thể đứng thẳng dậy.
               * Thở mạnh ra bằng miệng khi lên đến đỉnh.
            
            ---
            📈 *Lịch sử luyện tập*:
            $workoutStatus
            """.trimIndent()
        }

        cleanInput.contains("plank") -> {
            """
            🧘 **KỸ THUẬT PLANK CHUẨN AI & CƠ BỤNG** 🧘
            
            Plank là bài tập cô lập tuyệt vời giúp gia cố nhóm cơ trung tâm (Core). Thực hiện đúng kỹ thuật để kích hoạt cơ bụng tối đa:
            
            1️⃣ **Tư thế chuẩn bị (Setup)**:
               * Tựa thân người trên khuỷu tay và cẳng tay, cùi chỏ đặt vuông góc ngay dưới vai.
               * Nhón mũi chân, nâng cơ thể lên khỏi thảm.
            2️⃣ **Giữ thẳng trục cơ thể**:
               * Đầu, vai, lưng, hông và gót chân phải tạo thành một đường thẳng tắp. 
               * *Lưu ý*: Không nâng mông quá cao hoặc để võng lưng dưới (gây đau thắt lưng).
            3️⃣ **Kích hoạt cơ bụng (Co-contraction)**:
               * Siết chặt cơ bụng (như chuẩn bị bị ai đó đấm vào bụng) và siết chặt cơ mông.
               * Thở đều, sâu qua mũi và miệng.
            
            *Hãy tập trước camera AI để đo thời gian giữ đúng tư thế (CORRECT) nhé!*
            
            ---
            📈 *Lịch sử luyện tập*:
            $workoutStatus
            """.trimIndent()
        }

        cleanInput.contains("chào") || cleanInput.contains("hello") || cleanInput.contains("hi") || cleanInput.contains("alo") -> {
            "Chào bạn! Tôi là FitAI - Trợ lý huấn luyện viên thể hình cá nhân của bạn. $workoutStatus Hôm nay bạn muốn tôi tư vấn về BMI, chế độ dinh dưỡng, kỹ thuật bài tập hay lên giáo án tập luyện?"
        }

        else -> {
            "Tôi đã ghi nhận câu hỏi của bạn về '$userInput'. Để đạt kết quả tối ưu, hãy duy trì tần suất tập đều đặn hàng ngày và chú ý độ chính xác tư thế (Accuracy %) được phân tích thời gian thực qua camera của tôi nhé!"
        }
    }
}

@Composable
fun ChatBubble(
    senderName: String,
    message: String,
    isCoach: Boolean
) {
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val themeTertiary = MaterialTheme.colorScheme.tertiary
    val themeError = MaterialTheme.colorScheme.error
    val themeSurface = MaterialTheme.colorScheme.surface

    val bubbleColor = if (isCoach) themeSurface else themePrimary.copy(alpha = 0.15f)
    val alignment = if (isCoach) Alignment.Start else Alignment.End
    val nameColor = if (isCoach) themePrimary else themeSecondary
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isCoach) 2.dp else 16.dp,
        bottomEnd = if (isCoach) 16.dp else 2.dp
    )
    val neonColors = if (isCoach) {
        listOf(themePrimary, themeTertiary, themePrimary)
    } else {
        listOf(themeSecondary, themeError, themeSecondary)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Text(
            text = if (isCoach) "🤖 $senderName" else "👤 $senderName",
            color = nameColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .cyberpunkNeonBorder(
                    colors = neonColors,
                    borderWidth = 1.dp,
                    shape = bubbleShape,
                    glowRadius = 3.dp
                )
                .padding(12.dp)
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
