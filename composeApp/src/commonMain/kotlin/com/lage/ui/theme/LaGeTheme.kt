package com.lage.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

// ── 拉歌 7 分类色板 ──────────────────────────────────────────
object LaGeColors {
    val Lyric = Color(0xFFFF6B6B)           // 珊瑚红 — 歌词
    val Melody = Color(0xFF9C27B0)          // 紫色 — 旋律
    val Harmony = Color(0xFF00BCD4)         // 青色 — 和声
    val Rhythm = Color(0xFFFFC107)          // 金色 — 节奏
    val Production = Color(0xFF2196F3)      // 蓝色 — 制作
    val Instrumentation = Color(0xFF4CAF50) // 绿色 — 编曲
    val Structure = Color(0xFFE91E63)       // 粉色 — 结构
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    surface = Color(0xFFFFFBFE),
    background = Color(0xFFFFFBFE),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    surface = Color(0xFF1C1B1F),
    background = Color(0xFF1C1B1F),
)

@Composable
fun LaGeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
