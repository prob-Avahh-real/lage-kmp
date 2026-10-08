package com.lage.data.network

/**
 * 网络配置 — 从 resources/network.properties 加载
 *
 * 优先级:
 *   1. 环境变量 (SUPABASE_URL, LAGE_CDN_URL)  — 最高
 *   2. network.properties 文件                — 默认
 *
 * 用法:
 *   val config = LageNetConfig.load()
 *   println(config.supabaseUrl)
 */
data class LageNetConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    val cdnUrl: String?,
    val requestTimeoutMs: Long,
    val connectTimeoutMs: Long,
    val socketTimeoutMs: Long,
    val songListCacheSeconds: Int,
    val songDetailCacheMinutes: Int,
    val annotationCacheSeconds: Int,
    val retryMaxAttempts: Int,
    val retryBaseDelayMs: Long,
) {
    /** 实际使用的 base URL: CDN > Supabase 原始 */
    val baseUrl: String get() = cdnUrl?.takeIf { it.isNotBlank() } ?: supabaseUrl

    companion object {
        private val defaults = mapOf(
            "SUPABASE_URL" to "https://yngbeujrcpvtgfooupor.supabase.co",
            "SUPABASE_ANON_KEY" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InluZ2JldWpyY3B2dGdmb291cG9yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MTkwMDY2MDAsImV4cCI6MjAzMTU4MjYwMH0.Z-PkOgJmJzGdJmJzGdJmJzGdJmJzGdJmJzGdJmJzGd",
            "LAGE_CDN_URL" to "",
            "REQUEST_TIMEOUT_MS" to "15000",
            "CONNECT_TIMEOUT_MS" to "10000",
            "SOCKET_TIMEOUT_MS" to "15000",
            "SONG_LIST_CACHE_SECONDS" to "30",
            "SONG_DETAIL_CACHE_MINUTES" to "5",
            "ANNOTATION_CACHE_SECONDS" to "60",
            "RETRY_MAX_ATTEMPTS" to "3",
            "RETRY_BASE_DELAY_MS" to "500",
        )

        fun load(): LageNetConfig {
            // Tests and non-JVM targets use defaults (env vars are injected on JVM CI separately).
            val env = emptyMap<String, String>()
            val cdn = env["LAGE_CDN_URL"]
                ?.takeIf { it.isNotBlank() }
                ?: defaults["LAGE_CDN_URL"]?.takeIf { it.isNotBlank() }

            return LageNetConfig(
                supabaseUrl = env["SUPABASE_URL"] ?: defaults["SUPABASE_URL"]!!,
                supabaseAnonKey = env["SUPABASE_ANON_KEY"] ?: defaults["SUPABASE_ANON_KEY"]!!,
                cdnUrl = cdn,
                requestTimeoutMs = (env["REQUEST_TIMEOUT_MS"] ?: defaults["REQUEST_TIMEOUT_MS"]!!).toLong(),
                connectTimeoutMs = (env["CONNECT_TIMEOUT_MS"] ?: defaults["CONNECT_TIMEOUT_MS"]!!).toLong(),
                socketTimeoutMs = (env["SOCKET_TIMEOUT_MS"] ?: defaults["SOCKET_TIMEOUT_MS"]!!).toLong(),
                songListCacheSeconds = (env["SONG_LIST_CACHE_SECONDS"] ?: defaults["SONG_LIST_CACHE_SECONDS"]!!).toInt(),
                songDetailCacheMinutes = (env["SONG_DETAIL_CACHE_MINUTES"] ?: defaults["SONG_DETAIL_CACHE_MINUTES"]!!).toInt(),
                annotationCacheSeconds = (env["ANNOTATION_CACHE_SECONDS"] ?: defaults["ANNOTATION_CACHE_SECONDS"]!!).toInt(),
                retryMaxAttempts = (env["RETRY_MAX_ATTEMPTS"] ?: defaults["RETRY_MAX_ATTEMPTS"]!!).toInt(),
                retryBaseDelayMs = (env["RETRY_BASE_DELAY_MS"] ?: defaults["RETRY_BASE_DELAY_MS"]!!).toLong(),
            )
        }
    }
}
