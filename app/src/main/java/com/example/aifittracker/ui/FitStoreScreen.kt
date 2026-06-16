package com.example.aifittracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aifittracker.model.StoreProduct
import com.example.aifittracker.db.UserVoucher
import com.example.aifittracker.db.FitDao
import com.example.aifittracker.ui.theme.cyberpunkNeonBorder
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitStoreScreen(
    userId: Int,
    fitCoinBalance: Int,
    onCoinsDeducted: (Int) -> Unit,
    fitDao: FitDao
) {
    var storeMode by remember { mutableStateOf("Shop") } // "Shop" or "Vouchers"
    var selectedCategory by remember { mutableStateOf("All") }
    var showSuccessDialog by remember { mutableStateOf<StoreProduct?>(null) }
    var showErrorDialog by remember { mutableStateOf<StoreProduct?>(null) }
    var selectedProductForDetail by remember { mutableStateOf<StoreProduct?>(null) }
    
    var purchasedVouchers by remember { mutableStateOf(listOf<UserVoucher>()) }
    var selectedVoucherForDialog by remember { mutableStateOf<UserVoucher?>(null) }

    LaunchedEffect(storeMode, showSuccessDialog) {
        purchasedVouchers = fitDao.getAllVouchers(userId)
    }

    val mockProducts = remember {
        listOf(
            StoreProduct("1", "Whey Gold Standard 2kg", "1,450,000đ", 150, "Nutrition", "Tăng trưởng cơ bắp tối ưu với 24g protein tinh khiết mỗi khẩu phần."),
            StoreProduct("2", "Dây Kháng Lực Ngũ Sắc", "250,000đ", 45, "Equipment", "Bộ 5 dây ngũ sắc kháng lực hỗ trợ tập luyện tại nhà hiệu quả."),
            StoreProduct("3", "Bình Nước SmartShake 3 Ngăn", "180,000đ", 25, "Equipment", "Bình lắc thông minh tiện lợi để đựng whey và vitamin bổ sung."),
            StoreProduct("4", "Voucher Nike 100,000đ", "Miễn phí", 50, "Voucher", "Giảm trực tiếp 100k cho hóa đơn mua sắm tại Nike Store."),
            StoreProduct("5", "Thẻ Tập Thử California 7 Ngày", "Miễn phí", 80, "Voucher", "Trải nghiệm miễn phí 7 ngày tập luyện tại hệ thống California Fitness."),
            StoreProduct("6", "Thảm Yoga TPE Cao Cấp 8mm", "350,000đ", 60, "Equipment", "Thảm cao su chống trơn trượt cực tốt, bảo vệ đầu gối và khớp.")
        )
    }

    val filteredProducts = if (selectedCategory == "All") {
        mockProducts
    } else {
        mockProducts.filter { it.category == selectedCategory }
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

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FITSTORE SHOP",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Đổi thói quen tập luyện lấy quà tặng",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- STORE MODE TABS (Cửa hàng vs Voucher của tôi) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
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
            val modes = listOf("Shop" to "CỬA HÀNG", "Vouchers" to "VOUCHER CỦA TÔI")
            modes.forEach { (modeId, modeLabel) ->
                val isSelected = storeMode == modeId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { storeMode = modeId }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modeLabel,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- WALLET BALANCE CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .cyberpunkNeonBorder(
                    borderWidth = 1.2.dp,
                    shape = RoundedCornerShape(24.dp),
                    glowRadius = 6.dp
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.85f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VÍ TÍCH LŨY FITCOIN",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "FitCoin",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "$fitCoinBalance",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " xu",
                            color = Color.LightGray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFD54F).copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "MOVE-TO-EARN",
                        color = Color(0xFFFFD54F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (storeMode == "Shop") {
            // --- FILTER CATEGORY TABS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.45f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val categories = listOf("All" to "Tất cả", "Equipment" to "Đồ tập", "Nutrition" to "Dinh dưỡng", "Voucher" to "Voucher")
                categories.forEach { (catId, catLabel) ->
                    val isSelected = selectedCategory == catId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedCategory = catId }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = catLabel,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- PRODUCTS GRID ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredProducts) { product ->
                    val categoryIcon = when (product.category) {
                        "Nutrition" -> Icons.Default.Fastfood
                        "Equipment" -> Icons.Default.FitnessCenter
                        else -> Icons.Default.ConfirmationNumber
                    }

                    Card(
                        onClick = { selectedProductForDetail = product },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .cyberpunkNeonBorder(
                                borderWidth = 1.dp,
                                shape = RoundedCornerShape(24.dp),
                                glowRadius = 4.dp
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                // Category Tag
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = product.name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = when (product.category) {
                                            "Nutrition" -> "Dinh dưỡng"
                                            "Equipment" -> "Thiết bị"
                                            else -> "Voucher"
                                        },
                                        color = Color.LightGray,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Name
                                Text(
                                    text = product.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Description
                                Text(
                                    text = product.description,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 13.sp
                                )
                            }

                            Column {
                                // Price information
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = product.priceCash,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = "FitCoin",
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${product.priceCoins}",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Buy button
                                Button(
                                    onClick = {
                                        if (fitCoinBalance >= product.priceCoins) {
                                            onCoinsDeducted(product.priceCoins)
                                            // Lưu vào cơ sở dữ liệu Room
                                            fitDao.insertVoucher(
                                                UserVoucher(
                                                    userId = userId,
                                                    title = product.name,
                                                    category = product.category,
                                                    code = "FIT-" + UUID.randomUUID().toString().take(8).uppercase()
                                                )
                                            )
                                            showSuccessDialog = product
                                        } else {
                                            showErrorDialog = product
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "ĐỔI QUÀ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- VOUCHERS LIST ---
            if (purchasedVouchers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Ví Voucher Trống",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tập luyện chăm chỉ kiếm FitCoins\nvà đổi những voucher hấp dẫn nhé!",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { storeMode = "Shop" },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ĐẾN CỬA HÀNG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(purchasedVouchers) { voucher ->
                        val categoryIcon = when (voucher.category) {
                            "Nutrition" -> Icons.Default.Fastfood
                            "Equipment" -> Icons.Default.FitnessCenter
                            else -> Icons.Default.ConfirmationNumber
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVoucherForDialog = voucher }
                                .cyberpunkNeonBorder(
                                    borderWidth = 1.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    glowRadius = 4.dp
                                ),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1321).copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = voucher.category,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = voucher.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Mã: ${voucher.code}",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Ngày đổi: ${formatTimestamp(voucher.timestamp)}",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00E5FF))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "XEM",
                                        color = Color(0xFF0A0E17),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Success dialog
    if (showSuccessDialog != null) {
        val product = showSuccessDialog!!
        AlertDialog(
            onDismissRequest = { showSuccessDialog = null },
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
                    text = "ĐỔI QUÀ THÀNH CÔNG! 🎉",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E676),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Chúc mừng bạn đã quy đổi thành công:",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vui lòng vào mục Voucher Của Tôi để xem mã QR hoặc mã code để nhận hàng / sử dụng dịch vụ.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("TUYỆT VỜI", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Error Dialog (Not enough coins)
    if (showErrorDialog != null) {
        val product = showErrorDialog!!
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
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
                    text = "CHƯA ĐỦ SỐ DƯ! ❌",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF007F),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Số dư FitCoins của bạn không đủ để đổi quà tặng này.",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "💡 Mẹo: Tiếp tục thực hiện các bài tập Squat, Push-up, hoặc Plank đúng tư thế để kiếm thêm xu!",
                        fontSize = 12.sp,
                        color = Color(0xFF00E5FF),
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F), contentColor = Color(0xFF06080C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("TẬP LUYỆN NGAY", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Voucher Detail Dialog (Shows QR Code and Code)
    if (selectedVoucherForDialog != null) {
        val voucher = selectedVoucherForDialog!!
        val clipboardManager = LocalClipboardManager.current
        var copiedText by remember { mutableStateOf(false) }

        val qrGrid = remember(voucher.code) {
            val random = java.util.Random(voucher.code.hashCode().toLong())
            val grid = Array(8) { BooleanArray(8) }
            for (i in 0 until 8) {
                for (j in 0 until 8) {
                    if (!((i < 3 && j < 3) || (i < 3 && j >= 5) || (i >= 5 && j < 3))) {
                        grid[i][j] = random.nextBoolean()
                    }
                }
            }
            grid
        }

        AlertDialog(
            onDismissRequest = {
                selectedVoucherForDialog = null
                copiedText = false
            },
            modifier = Modifier
                .padding(16.dp)
                .cyberpunkNeonBorder(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFFFF007F), Color(0xFF9D4EDD), Color(0xFF00E5FF)),
                    borderWidth = 1.5.dp,
                    shape = RoundedCornerShape(24.dp),
                    glowRadius = 12.dp
                ),
            containerColor = Color(0xFF06080C),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "CHI TIẾT VOUCHER",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = voucher.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Canvas-drawn QR Code!
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val size = this.size.width
                            val cellSize = size / 8f
                            // Corners (QR Code Finder patterns)
                            val finderSize = 3 * cellSize

                            // Top-left finder
                            drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(finderSize, finderSize))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(cellSize, cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                            // Top-right finder
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(size - finderSize, 0f), size = androidx.compose.ui.geometry.Size(finderSize, finderSize))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(size - finderSize + cellSize, cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                            // Bottom-left finder
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, size - finderSize), size = androidx.compose.ui.geometry.Size(finderSize, finderSize))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(cellSize, size - finderSize + cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))

                            // Random data patterns using a cached grid
                            for (i in 0 until 8) {
                                for (j in 0 until 8) {
                                    // Skip finder patterns
                                    if ((i < 3 && j < 3) || (i < 3 && j >= 5) || (i >= 5 && j < 3)) {
                                        continue
                                    }
                                    if (qrGrid[i][j]) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = androidx.compose.ui.geometry.Offset(i * cellSize, j * cellSize),
                                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                        )
                                    }
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    // Alphanumeric Code Display with Copy Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D1321))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = voucher.code,
                            color = Color(0xFFFFD54F),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = if (copiedText) "ĐÃ SAO CHÉP" else "SAO CHÉP MÃ",
                            color = if (copiedText) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(voucher.code))
                                    copiedText = true
                                }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Usage Instructions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Hướng Dẫn Sử Dụng:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Đưa mã QR hoặc mã code này cho nhân viên tại quầy thu ngân để áp dụng.\n• Voucher áp dụng một lần cho mỗi hóa đơn.\n• Hạn sử dụng: 30 ngày kể từ ngày quy đổi (${formatTimestamp(voucher.timestamp + 30L * 24 * 60 * 60 * 1000)}).",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedVoucherForDialog = null
                        copiedText = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F), contentColor = Color(0xFF06080C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ĐÓNG", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (selectedProductForDetail != null) {
        val product = selectedProductForDetail!!
        var isProcessing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        
        AlertDialog(
            onDismissRequest = { if (!isProcessing) selectedProductForDetail = null },
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
                    text = "CHI TIẾT SẢN PHẨM",
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val categoryIcon = when (product.category) {
                            "Nutrition" -> Icons.Default.Fastfood
                            "Equipment" -> Icons.Default.FitnessCenter
                            else -> Icons.Default.ConfirmationNumber
                        }
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = product.category,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) {
                            Text("⭐", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.9/5 • 120+ lượt đổi",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = product.description,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0D1321))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Phân loại:", color = Color.Gray, fontSize = 11.sp)
                            Text(
                                text = when (product.category) {
                                    "Nutrition" -> "Thực Phẩm Bổ Sung"
                                    "Equipment" -> "Dụng Cụ Tập Luyện"
                                    else -> "Mã Ưu Đãi Đổi Quà"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Giá trị quy đổi gốc:", color = Color.Gray, fontSize = 11.sp)
                            Text(product.priceCash, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tình trạng kho hàng:", color = Color.Gray, fontSize = 11.sp)
                            Text("CÒN HÀNG (Sẵn sàng giao)", color = Color(0xFF39FF14), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Yêu cầu: ", color = Color.Gray, fontSize = 13.sp)
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "FitCoins",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${product.priceCoins} FitCoins",
                            color = Color(0xFFFFD54F),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    if (isProcessing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Đang kết nối hệ thống ví...",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isProcessing) {
                            if (fitCoinBalance >= product.priceCoins) {
                                isProcessing = true
                                coroutineScope.launch {
                                    delay(1500) // cool processing animation
                                    isProcessing = false
                                    selectedProductForDetail = null
                                    
                                    // Complete exchange
                                    onCoinsDeducted(product.priceCoins)
                                    fitDao.insertVoucher(
                                        UserVoucher(
                                            userId = userId,
                                            title = product.name,
                                            category = product.category,
                                            code = "FIT-" + UUID.randomUUID().toString().take(8).uppercase()
                                        )
                                    )
                                    showSuccessDialog = product
                                }
                            } else {
                                selectedProductForDetail = null
                                showErrorDialog = product
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (fitCoinBalance >= product.priceCoins) MaterialTheme.colorScheme.primary else Color.Gray,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isProcessing
                ) {
                    Text(
                        text = if (fitCoinBalance >= product.priceCoins) "ĐỔI QUÀ NGAY" else "KHÔNG ĐỦ XU",
                        fontWeight = FontWeight.Black
                    )
                }
            },
            dismissButton = {
                if (!isProcessing) {
                    TextButton(onClick = { selectedProductForDetail = null }) {
                        Text("QUAY LẠI", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
