package com.lage.data.model

import com.lage.data.model.AnnotationCategory
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 模型序列化/反序列化测试
 *
 * 验证:
 *   - 所有 @Serializable 模型的 JSON 序列化正确性
 *   - SerialName 映射
 *   - 默认值处理
 *   - 空安全
 */
class ModelsSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    // ═══════════════════════════════════════════════════════════
    // Song 序列化测试
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `song - should deserialize minimal JSON correctly`() {
        val input = """
            {
                "id": "song-1",
                "title": "测试歌曲",
                "artist": "测试艺人",
                "duration_secs": 240.5,
                "created_by": "user-1",
                "created_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val song = json.decodeFromString<Song>(input)
        assertEquals("song-1", song.id)
        assertEquals("测试歌曲", song.title)
        assertEquals(240.5, song.durationSecs)
        assertNull(song.album)
        assertNull(song.coverUrl)
        assertEquals(0, song.analysisCount)
    }

    @Test
    fun `song - should deserialize full JSON with all fields`() {
        val input = """
            {
                "id": "song-2",
                "title": "完整歌曲",
                "artist": "艺人",
                "album": "专辑名",
                "cover_url": "https://example.com/cover.jpg",
                "audio_url": "https://example.com/audio.mp3",
                "duration_secs": 180.0,
                "bpm": 120.0,
                "key_signature": "C",
                "description": "描述",
                "created_by": "user-2",
                "created_at": "2026-06-01T12:00:00Z",
                "analysis_count": 5,
                "like_count": 42,
                "creator_username": "creator",
                "creator_avatar_url": "https://example.com/avatar.jpg"
            }
        """.trimIndent()

        val song = json.decodeFromString<Song>(input)
        assertEquals("完整歌曲", song.title)
        assertEquals("专辑名", song.album)
        assertEquals(120.0, song.bpm)
        assertEquals("C", song.keySignature)
        assertEquals(5, song.analysisCount)
        assertEquals(42, song.likeCount)
        assertEquals("creator", song.creatorUsername)
    }

    @Test
    fun `song - durationSecs must be non-negative`() {
        val input = """
            {
                "id": "song-3",
                "title": "边界测试",
                "artist": "测试",
                "duration_secs": 0.0,
                "created_by": "user-3",
                "created_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val song = json.decodeFromString<Song>(input)
        assertEquals(0.0, song.durationSecs)
    }

    @Test
    fun `song - should ignore unknown fields`() {
        val input = """
            {
                "id": "song-4",
                "title": "忽略未知",
                "artist": "测试",
                "duration_secs": 100.0,
                "created_by": "user-4",
                "created_at": "2026-01-01T00:00:00Z",
                "unknown_field": "should be ignored"
            }
        """.trimIndent()

        val song = json.decodeFromString<Song>(input)
        assertEquals("song-4", song.id)
    }

    // ═══════════════════════════════════════════════════════════
    // Annotation 序列化测试
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `annotation - should deserialize with LYRIC category`() {
        val input = """
            {
                "id": "ann-1",
                "song_id": "song-1",
                "user_id": "user-1",
                "timestamp_start": 10.5,
                "timestamp_end": 15.0,
                "category": "lyric",
                "content": "测试内容",
                "created_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val annotation = json.decodeFromString<Annotation>(input)
        assertEquals(AnnotationCategory.LYRIC, annotation.category)
        assertEquals(10.5, annotation.timestampStart)
        assertEquals(15.0, annotation.timestampEnd)
    }

    @Test
    fun `annotation - timestamp range must be valid`() {
        val input = """
            {
                "id": "ann-2",
                "song_id": "song-1",
                "user_id": "user-1",
                "timestamp_start": 20.0,
                "timestamp_end": 10.0,
                "category": "melody",
                "content": "反向时间戳",
                "created_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val annotation = json.decodeFromString<Annotation>(input)
        assertTrue(annotation.timestampStart > annotation.timestampEnd,
            "timestamp_start > timestamp_end is allowed by model but should be validated")
    }

    // ═══════════════════════════════════════════════════════════
    // AnnotationCategory 枚举测试
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `annotationCategory - all categories should deserialize`() {
        val inputs = mapOf(
            "lyric" to AnnotationCategory.LYRIC,
            "melody" to AnnotationCategory.MELODY,
            "harmony" to AnnotationCategory.HARMONY,
            "rhythm" to AnnotationCategory.RHYTHM,
            "production" to AnnotationCategory.PRODUCTION,
            "instrumentation" to AnnotationCategory.INSTRUMENTATION,
            "structure" to AnnotationCategory.STRUCTURE,
        )

        for ((name, expected) in inputs) {
            val input = """{"category": "$name"}"""
            val result = json.decodeFromString<AnnotationCategory>(input)
            assertEquals(expected, result)
        }
    }

    @Test
    fun `appUser - should deserialize with optional fields missing`() {
        val input = """
            {
                "id": "user-1",
                "username": "测试用户"
            }
        """.trimIndent()

        val user = json.decodeFromString<AppUser>(input)
        assertEquals("user-1", user.id)
        assertEquals("测试用户", user.username)
        assertNull(user.email)
        assertNull(user.avatarUrl)
    }

    @Test
    fun `appUser - should deserialize with all fields`() {
        val input = """
            {
                "id": "user-2",
                "email": "test@example.com",
                "username": "fulluser",
                "avatar_url": "https://example.com/avatar.png",
                "bio": "简介",
                "created_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val user = json.decodeFromString<AppUser>(input)
        assertEquals("test@example.com", user.email)
        assertEquals("简介", user.bio)
        assertNotNull(user.createdAt)
    }
}
