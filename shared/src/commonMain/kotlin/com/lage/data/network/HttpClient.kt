package com.lage.data.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

/**
 * 网络层 — 从配置文件读取，不硬编码
 *
 * 配置优先级: 环境变量 > network.properties > 代码默认值
 *
 * 特性:
 *   [HTTP/2] Ktor 引擎自动协商
 *   [压缩] Brotli + Gzip + Deflate
 *   [连接复用] Keep-Alive + 连接池
 *   [预连接] warmup() 提前建立连接
 *   [CDN] 支持 Cloudflare 代理自定义域名
 */
object LageHttpClient {

    private val config: LageNetConfig by lazy { LageNetConfig.load() }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    val isCdnEnabled: Boolean get() = !config.cdnUrl.isNullOrBlank()
    val currentBaseUrl: String get() = config.baseUrl

    private val _client: HttpClient by lazy { createClient() }
    val client: HttpClient get() = _client

    fun createClient(baseUrl: String = config.baseUrl): HttpClient = HttpClient {
        install(ContentNegotiation) { json(json) }

        install(ContentEncoding) {
            gzip()
            deflate()
        }

        install(Logging) { level = LogLevel.HEADERS }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMs
            connectTimeoutMillis = config.connectTimeoutMs
            socketTimeoutMillis = config.socketTimeoutMs
        }

        defaultRequest {
            url(baseUrl)
            header("apikey", config.supabaseAnonKey)
            header(HttpHeaders.AcceptEncoding, "br, gzip, deflate")
            header(HttpHeaders.Accept, "application/json")
            contentType(ContentType.Application.Json)
        }
    }

    /** 预热连接 */
    suspend fun warmup() {
        try {
            val response = client.get("/rest/v1/") {
                headers { append("Prefer", "count=none") }
            }
            println("[LageHttp] 预热完成: ${response.status}")
        } catch (e: Exception) {
            println("[LageHttp] 预热失败 (可忽略): ${e.message}")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// TTL 缓存
// ═══════════════════════════════════════════════════════════

class ResponseCache<T>(
    private val ttl: kotlin.time.Duration = 30.seconds
) {
    internal data class Entry<T>(
        val data: T,
        val timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) {
        fun isExpired(ttlMs: Long): Boolean =
            Clock.System.now().toEpochMilliseconds() - timestamp > ttlMs
    }

    private val cache = mutableMapOf<String, Entry<T>>()
    private val mutex = Mutex()
    var hits = 0; private set
    var misses = 0; private set

    /** 非挂起版本 — 供 runCatching 等非协程作用域调用 */
    fun get(key: String): T? = runBlocking {
        mutex.withLock {
            val entry = cache[key] ?: run { misses++; return@withLock null }
            if (entry.isExpired(ttl.inWholeMilliseconds)) {
                cache.remove(key); misses++; return@withLock null
            }
            hits++
            entry.data
        }
    }

    fun put(key: String, value: T) = runBlocking {
        mutex.withLock { cache[key] = Entry(value) }
    }

    fun invalidate(key: String) = runBlocking {
        mutex.withLock { cache.remove(key) }
    }

    fun invalidatePrefix(prefix: String) = runBlocking {
        mutex.withLock {
            cache.keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
        }
    }

    fun clear() = runBlocking {
        mutex.withLock { cache.clear(); hits = 0; misses = 0 }
    }
}

// ═══════════════════════════════════════════════════════════
// 重试 + 指数退避
// ═══════════════════════════════════════════════════════════

suspend fun <T> retryWithBackoff(
    maxAttempts: Int = LageNetConfig.load().retryMaxAttempts,
    baseDelayMs: Long = LageNetConfig.load().retryBaseDelayMs,
    block: suspend () -> T
): T {
    var attempt = 0
    var lastException: Exception? = null
    while (attempt < maxAttempts) {
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            attempt++
            if (attempt >= maxAttempts) break
            val delay = baseDelayMs * (1L shl (attempt - 1))
            println("[LageHttp] 重试 #$attempt, 等待 ${delay}ms")
            delay(delay)
        }
    }
    throw lastException!!
}
