package com.lage.data.repository

import com.lage.data.model.*
import com.lage.data.network.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AnnotationRepository {
    private val client = LageHttpClient.client
    private val cache = ResponseCache<List<Annotation>>(kotlin.time.Duration.Companion.seconds(60))

    /** 获取歌曲标注列表 */
    suspend fun getAnnotations(songId: String, forceRefresh: Boolean = false): Result<List<Annotation>> {
        val cacheKey = "annotations:$songId"
        if (!forceRefresh) {
            cache.get(cacheKey)?.let { return Result.success(it) }
        }

        return runCatching {
            val response = client.get("/rest/v1/annotations") {
                parameter("select", "*,profiles!annotations_user_id_fkey(username,avatar_url)")
                parameter("song_id", "eq.$songId")
                parameter("order", "timestamp_start.asc")
            }
            val annotations: List<Annotation> = response.body()
            cache.put(cacheKey, annotations)
            annotations
        }
    }

    /** 创建标注 */
    suspend fun createAnnotation(
        songId: String, userId: String,
        timestampStart: Double, timestampEnd: Double,
        category: AnnotationCategory,
        title: String?, content: String, color: String?,
    ): Result<Annotation> = runCatching {
        val now = kotlinx.datetime.Clock.System.now().toString()
        val response = client.post("/rest/v1/annotations") {
            setBody(mapOf(
                "song_id" to songId, "user_id" to userId,
                "timestamp_start" to timestampStart, "timestamp_end" to timestampEnd,
                "category" to category.name.lowercase(),
                "title" to title, "content" to content, "color" to color,
                "created_at" to now, "updated_at" to now,
            ))
            headers { append("Prefer", "return=representation") }
        }
        val annotation: Annotation = response.body()
        // 增加分析计数
        client.post("/rest/v1/rpc/increment_analysis_count") {
            setBody(mapOf("song_id" to songId))
        }
        cache.invalidate("annotations:$songId")
        annotation
    }

    /** 删除标注 */
    suspend fun deleteAnnotation(annotationId: String, songId: String): Result<Unit> = runCatching {
        client.delete("/rest/v1/annotations") {
            parameter("id", "eq.$annotationId")
        }
        client.post("/rest/v1/rpc/decrement_analysis_count") {
            setBody(mapOf("song_id" to songId))
        }
        cache.invalidate("annotations:$songId")
    }

    fun clearCache() = cache.clear()
}
