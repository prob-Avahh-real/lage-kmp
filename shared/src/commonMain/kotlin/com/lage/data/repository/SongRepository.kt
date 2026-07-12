package com.lage.data.repository

import com.lage.data.model.*
import com.lage.data.network.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * 歌曲仓库 — 封装 Supabase REST API
 *
 * 性能优化:
 *   - 列表 30s TTL 缓存，详情 5min TTL 缓存
 *   - 写操作 (create/toggleLike) 自动清除相关缓存
 *   - 支持分页 (offset/limit)
 */
class SongRepository {
    private val client = LageHttpClient.client
    private val listCache = ResponseCache<List<Song>>(kotlin.time.Duration.Companion.seconds(30))
    private val detailCache = ResponseCache<Song>(kotlin.time.Duration.Companion.minutes(5))

    /** 获取歌曲列表 */
    suspend fun getSongs(
        sortBy: String = "latest",
        limit: Int = 20,
        offset: Int = 0,
        forceRefresh: Boolean = false,
    ): Result<List<Song>> {
        val cacheKey = "songs:$sortBy:$offset"
        if (!forceRefresh) {
            listCache.get(cacheKey)?.let { return Result.success(it) }
        }

        return runCatching {
            val orderCol = when (sortBy) {
                "popular" -> "like_count"
                "analyzed" -> "analysis_count"
                else -> "created_at"
            }

            val response = client.get("/rest/v1/songs") {
                headers {
                    append("Prefer", "count=exact")
                }
                parameter("select", "*,profiles!songs_created_by_fkey(username,avatar_url)")
                parameter("order", "$orderCol.desc")
                parameter("limit", limit)
                parameter("offset", offset)
            }

            val songs: List<Song> = response.body()
            listCache.put(cacheKey, songs)
            songs
        }
    }

    /** 获取单首歌曲详情 */
    suspend fun getSong(songId: String, forceRefresh: Boolean = false): Result<Song> {
        val cacheKey = "song:$songId"
        if (!forceRefresh) {
            detailCache.get(cacheKey)?.let { return Result.success(it) }
        }

        return runCatching {
            val response = client.get("/rest/v1/songs") {
                parameter("select", "*,profiles!songs_created_by_fkey(username,avatar_url)")
                parameter("id", "eq.$songId")
                headers { append("Accept", "application/vnd.pgrst.object+json") }
            }
            val song: Song = response.body()
            detailCache.put(cacheKey, song)
            song
        }
    }

    /** 搜索歌曲 */
    suspend fun searchSongs(query: String): Result<List<Song>> {
        return runCatching {
            val response = client.get("/rest/v1/songs") {
                parameter("select", "*,profiles!songs_created_by_fkey(username,avatar_url)")
                parameter("or", "(title.ilike.*$query*,artist.ilike.*$query*)")
                parameter("limit", 20)
            }
            response.body()
        }
    }

    /** 切换点赞 */
    suspend fun toggleLike(songId: String, userId: String): Result<Boolean> {
        return runCatching {
            // 查询现有点赞
            val existing = client.get("/rest/v1/song_likes") {
                parameter("select", "id")
                parameter("song_id", "eq.$songId")
                parameter("user_id", "eq.$userId")
                headers { append("Accept", "application/vnd.pgrst.object+json") }
            }

            if (existing.status == HttpStatusCode.OK) {
                // 取消点赞
                client.delete("/rest/v1/song_likes") {
                    parameter("song_id", "eq.$songId")
                    parameter("user_id", "eq.$userId")
                }
                client.post("/rest/v1/rpc/decrement_like_count") {
                    setBody(mapOf("song_id" to songId))
                }
                // 清除缓存
                detailCache.invalidate("song:$songId")
                listCache.invalidatePrefix("songs:")
                false
            } else {
                // 点赞
                client.post("/rest/v1/song_likes") {
                    setBody(mapOf(
                        "song_id" to songId,
                        "user_id" to userId,
                        "created_at" to kotlinx.datetime.Clock.System.now().toString()
                    ))
                }
                client.post("/rest/v1/rpc/increment_like_count") {
                    setBody(mapOf("song_id" to songId))
                }
                detailCache.invalidate("song:$songId")
                listCache.invalidatePrefix("songs:")
                true
            }
        }
    }

    /** 清除全部缓存 */
    fun clearCache() {
        listCache.clear()
        detailCache.clear()
    }

    fun cacheStats(): Map<String, Int> = mapOf(
        "list_hits" to listCache.hits,
        "list_misses" to listCache.misses,
        "detail_hits" to detailCache.hits,
        "detail_misses" to detailCache.misses,
    )
}
