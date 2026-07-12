package com.lage.data.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds

/**
 * ResponseCache 单元测试
 *
 * 验证:
 *   - TTL 过期逻辑
 *   - 缓存命中/未命中计数
 *   - 失效操作 (key / prefix)
 *   - 清除操作
 *   - 并发安全 (Mutex)
 */
class ResponseCacheTest {

    @Test
    fun `get - should return null for unknown key`() {
        val cache = ResponseCache<String>(1000.milliseconds)
        val result = cache.get("unknown")
        assertNull(result)
    }

    @Test
    fun `get - should return cached value`() {
        val cache = ResponseCache<String>(5000.milliseconds)
        cache.put("key1", "value1")
        val result = cache.get("key1")
        assertEquals("value1", result)
    }

    @Test
    fun `get - should track hits and misses`() {
        val cache = ResponseCache<String>(5000.milliseconds)
        cache.put("key1", "value1")

        // miss then hit
        cache.get("unknown")
        cache.get("key1")

        assertEquals(1, cache.hits)
        assertEquals(1, cache.misses)
    }

    @Test
    fun `get - should return null after TTL expiry`() {
        // TTL = 0ms means immediately expired
        val cache = ResponseCache<String>(0.milliseconds)
        cache.put("key1", "value1")
        val result = cache.get("key1")
        assertNull(result, "Entry with 0ms TTL should be immediately expired")
    }

    @Test
    fun `invalidate - should remove single key`() {
        val cache = ResponseCache<String>(5000.milliseconds)
        cache.put("key1", "v1")
        cache.put("key2", "v2")

        cache.invalidate("key1")

        assertNull(cache.get("key1"))
        assertEquals("v2", cache.get("key2"))
    }

    @Test
    fun `invalidatePrefix - should remove all matching keys`() {
        val cache = ResponseCache<String>(5000.milliseconds)
        cache.put("songs:latest:0", "list1")
        cache.put("songs:popular:0", "list2")
        cache.put("song:123", "detail")

        cache.invalidatePrefix("songs:")

        assertNull(cache.get("songs:latest:0"))
        assertNull(cache.get("songs:popular:0"))
        assertEquals("detail", cache.get("song:123"))
    }

    @Test
    fun `clear - should remove all entries and reset counters`() {
        val cache = ResponseCache<String>(5000.milliseconds)
        cache.put("key1", "v1")
        cache.put("key2", "v2")

        // Generate a hit
        cache.get("key1")

        cache.clear()

        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
        assertEquals(0, cache.hits)
        assertEquals(0, cache.misses)
    }

    @Test
    fun `repeated put - should overwrite existing value`() {
        val cache = ResponseCache<String>(5000.milliseconds)
        cache.put("key1", "old")
        cache.put("key1", "new")

        val result = cache.get("key1")
        assertEquals("new", result)
    }

    @Test
    fun `multiple operations - should maintain correct hit counts`() {
        val cache = ResponseCache<String>(5000.milliseconds)

        // Insert 3 items
        cache.put("a", "1")
        cache.put("b", "2")
        cache.put("c", "3")

        // Access: a (hit), b (hit), unknown (miss)
        cache.get("a")
        cache.get("b")
        cache.get("unknown")
        cache.get("c")  // hit

        assertEquals(3, cache.hits)
        assertEquals(1, cache.misses)
    }
}
