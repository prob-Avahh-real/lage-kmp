package com.lage.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("duration_secs") val durationSecs: Double,
    val bpm: Double? = null,
    @SerialName("key_signature") val keySignature: String? = null,
    val description: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("analysis_count") val analysisCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    // Joined from profiles
    @SerialName("creator_username") val creatorUsername: String? = null,
    @SerialName("creator_avatar_url") val creatorAvatarUrl: String? = null,
)

@Serializable
data class Annotation(
    val id: String,
    @SerialName("song_id") val songId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("timestamp_start") val timestampStart: Double,
    @SerialName("timestamp_end") val timestampEnd: Double,
    val category: AnnotationCategory,
    val title: String? = null,
    val content: String = "",
    val color: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    // Joined from profiles
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
enum class AnnotationCategory {
    @SerialName("lyric")          LYRIC,
    @SerialName("melody")        MELODY,
    @SerialName("harmony")       HARMONY,
    @SerialName("rhythm")        RHYTHM,
    @SerialName("production")    PRODUCTION,
    @SerialName("instrumentation") INSTRUMENTATION,
    @SerialName("structure")     STRUCTURE,
}

@Serializable
data class AppUser(
    val id: String,
    val email: String? = null,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
