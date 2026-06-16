package com.example.aifittracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.aifittracker.ui.MainScreen
import com.example.aifittracker.ui.theme.AIFitTrackerTheme
import com.example.aifittracker.net.SocketManager
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, UI will update accordingly
        } else {
            // Explain to the user that the feature is unavailable
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkCameraPermission()
        
        // Load cấu hình kết nối đã lưu và khởi động kết nối WebSocket
        SocketManager.loadSettings(this)
        SocketManager.connect()

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPref = remember { context.getSharedPreferences("fit_tracker_prefs", android.content.Context.MODE_PRIVATE) }
            var currentTheme by remember { mutableStateOf(sharedPref.getString("app_theme", "CYBERPUNK") ?: "CYBERPUNK") }

            AIFitTrackerTheme(themeName = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        currentTheme = currentTheme,
                        onThemeChanged = { newTheme ->
                            currentTheme = newTheme
                            sharedPref.edit().putString("app_theme", newTheme).apply()
                        }
                    )
                }
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
