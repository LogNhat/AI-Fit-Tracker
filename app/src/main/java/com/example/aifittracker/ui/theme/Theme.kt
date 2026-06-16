package com.example.aifittracker.ui.theme

import android.graphics.Matrix
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppThemeColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val borderGradient: List<Color>
)

val CyberpunkColors = AppThemeColors(
    primary = Color(0xFF00E5FF),     // Neon Cyan
    secondary = Color(0xFF39FF14),   // Acid Green
    tertiary = Color(0xFFFFEA00),   // Laser Yellow
    accent = Color(0xFFFF007F),     // Hot Pink
    background = Color(0xFF06080C), // Deep Obsidian Black
    surface = Color(0xFF0D1321),    // Dark Blue Card
    borderGradient = listOf(Color(0xFF00E5FF), Color(0xFFFF007F), Color(0xFF9D4EDD), Color(0xFF00E5FF))
)

val SunsetColors = AppThemeColors(
    primary = Color(0xFFFF9100),     // Neon Orange
    secondary = Color(0xFFFFEA00),   // Laser Yellow
    tertiary = Color(0xFFFF5252),   // Bright Red
    accent = Color(0xFFFF3D00),     // Deep Orange
    background = Color(0xFF0F0B06), // Dark Warm Espresso
    surface = Color(0xFF1C130D),    // Warm Card Surface
    borderGradient = listOf(Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFFFF3D00), Color(0xFFFF9100))
)

val MatrixColors = AppThemeColors(
    primary = Color(0xFF00FF66),     // Acid green
    secondary = Color(0xFF00E5FF),   // Cyber Cyan
    tertiary = Color(0xFF39FF14),   // Neon Green
    accent = Color(0xFFFF003C),     // Neon Red
    background = Color(0xFF040A06), // Digital Terminal Green-Black
    surface = Color(0xFF0A150D),    // Dark Green Card
    borderGradient = listOf(Color(0xFF00FF66), Color(0xFF00E5FF), Color(0xFF39FF14), Color(0xFF00FF66))
)

val VaporwaveColors = AppThemeColors(
    primary = Color(0xFF9D4EDD),     // Neon Purple
    secondary = Color(0xFFFF007F),   // Hot Pink
    tertiary = Color(0xFF00E5FF),   // Electric Cyan
    accent = Color(0xFFFF5252),     // Accent Pinkish Red
    background = Color(0xFF0C0614), // Dark Violet Black
    surface = Color(0xFF140D21),    // Purple Card
    borderGradient = listOf(Color(0xFF9D4EDD), Color(0xFFFF007F), Color(0xFF00E5FF), Color(0xFF9D4EDD))
)

val LocalAppThemeColors = staticCompositionLocalOf { CyberpunkColors }

object FitTrackerTheme {
    val colors: AppThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemeColors.current
}

@Composable
fun AIFitTrackerTheme(
    themeName: String = "CYBERPUNK",
    content: @Composable () -> Unit
) {
    val appThemeColors = when (themeName) {
        "SUNSET" -> SunsetColors
        "MATRIX" -> MatrixColors
        "VAPORWAVE" -> VaporwaveColors
        else -> CyberpunkColors
    }

    val colorScheme = darkColorScheme(
        primary = appThemeColors.primary,
        secondary = appThemeColors.secondary,
        tertiary = appThemeColors.tertiary,
        background = appThemeColors.background,
        surface = appThemeColors.surface,
        error = appThemeColors.accent,
        onPrimary = Color(0xFF06080C),
        onSecondary = Color(0xFF06080C),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF)
    )

    CompositionLocalProvider(LocalAppThemeColors provides appThemeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

class RotatingSweepGradientBrush(
    private val colors: List<Color>,
    private val rotation: Float
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val shader = SweepGradient(
            size.width / 2f,
            size.height / 2f,
            colors.map { it.toArgb() }.toIntArray(),
            null
        )
        val matrix = Matrix()
        matrix.postRotate(rotation, size.width / 2f, size.height / 2f)
        shader.setLocalMatrix(matrix)
        return shader
    }
}

fun Modifier.cyberpunkNeonBorder(
    colors: List<Color>? = null,
    borderWidth: Dp = 1.5.dp,
    shape: Shape = RoundedCornerShape(24.dp),
    glowRadius: Dp = 8.dp
): Modifier = composed {
    val themeColors = FitTrackerTheme.colors
    val borderColors = colors ?: themeColors.borderGradient
    
    val infiniteTransition = rememberInfiniteTransition(label = "neon_border_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    this
        .graphicsLayer {
            shadowElevation = glowRadius.toPx()
            this.shape = shape
            clip = false
            ambientShadowColor = borderColors.first().copy(alpha = 0.8f)
            spotShadowColor = borderColors.getOrNull(1)?.copy(alpha = 0.8f) ?: borderColors.first().copy(alpha = 0.8f)
        }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawWithContent {
                drawContent()
                val brush = RotatingSweepGradientBrush(borderColors, rotation)
                when (outline) {
                    is Outline.Rectangle -> {
                        drawRect(
                            brush = brush,
                            style = Stroke(borderWidth.toPx())
                        )
                    }
                    is Outline.Rounded -> {
                        drawOutline(
                            outline = outline,
                            brush = brush,
                            style = Stroke(borderWidth.toPx())
                        )
                    }
                    is Outline.Generic -> {
                        drawOutline(
                            outline = outline,
                            brush = brush,
                            style = Stroke(borderWidth.toPx())
                        )
                    }
                }
            }
        }
}



