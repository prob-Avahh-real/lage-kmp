package com.lage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lage.data.network.LageHttpClient
import com.lage.ui.home.HomeScreen
import com.lage.ui.theme.LaGeTheme
import kotlinx.coroutines.launch

/**
 * App 入口 — Compose Multiplatform
 *
 * 启动时:
 *   1. 启用 Cloudflare CDN 代理 (如果配置了 Custom Domain)
 *   2. 安装 LaGeTheme (Material3)
 *   3. 预热 Supabase 连接 (DNS + TCP + TLS)
 *   4. 加载首页 — HomeScreen
 *
 * Cloudflare CDN 配置:
 *   - 设置环境变量 LAGE_CDN_URL=https://api.yourdomain.com
 *   - 或调用 LageHttpClient.enableCdn("https://api.yourdomain.com") 在 init 中
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    var warmupDone by remember { mutableStateOf(false) }

    LaGeTheme {
        LaunchedEffect(Unit) {
            // CDN 自动从 network.properties 读取，无需手动 enableCdn
            val cfg = com.lage.data.network.LageNetConfig.load()
            if (!cfg.cdnUrl.isNullOrBlank()) {
                println("[LaGe] Cloudflare CDN 已启用: ${cfg.cdnUrl}")
            }

            LageHttpClient.warmup()
            warmupDone = true
        }

        Surface(Modifier.fillMaxSize()) {
            if (!warmupDone) {
                // 闪屏 — 预热期间展示
                SplashContent()
            } else {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home",
                ) {
                    composable("home") {
                        HomeScreen(
                            onSongClick = { songId ->
                                navController.navigate("player/$songId")
                            },
                        )
                    }
                    composable("player/{songId}") { backStackEntry ->
                        val songId = backStackEntry.arguments?.getString("songId") ?: return@composable
                        // PlayerScreen 将在后续实现
                        PlayerPlaceholder(songId)
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashContent() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

@Composable
private fun PlayerPlaceholder(songId: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Text("正在加载歌曲...")
    }
}
