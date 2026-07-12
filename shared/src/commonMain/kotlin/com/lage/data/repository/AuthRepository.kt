package com.lage.data.repository

import com.lage.data.model.*
import com.lage.data.network.LageHttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthRepository {
    private val client = LageHttpClient.client

    /** 注册 */
    suspend fun signUp(email: String, password: String, username: String): Result<Unit> = runCatching {
        val response = client.post("/auth/v1/signup") {
            setBody(mapOf("email" to email, "password" to password, "data" to mapOf("username" to username)))
        }
        check(response.status.isSuccess()) { "注册失败: ${response.status}" }
    }

    /** 登录 */
    suspend fun signIn(email: String, password: String): Result<Pair<String, String>> = runCatching {
        val response = client.post("/auth/v1/token?grant_type=password") {
            setBody(mapOf("email" to email, "password" to password))
        }
        val body: Map<String, String> = response.body()
        val accessToken = body["access_token"] ?: throw IllegalStateException("登录响应缺少 access_token")
        val refreshToken = body["refresh_token"] ?: throw IllegalStateException("登录响应缺少 refresh_token")
        accessToken to refreshToken
    }

    /** 获取用户资料 */
    suspend fun getProfile(userId: String): Result<AppUser> = runCatching {
        val response = client.get("/rest/v1/profiles") {
            parameter("id", "eq.$userId")
            headers { append("Accept", "application/vnd.pgrst.object+json") }
        }
        response.body()
    }

    /** 更新资料 */
    suspend fun updateProfile(userId: String, username: String?, avatarUrl: String?, bio: String?): Result<Unit> =
        runCatching {
            val updates = mutableMapOf<String, String>()
            username?.let { updates["username"] = it }
            avatarUrl?.let { updates["avatar_url"] = it }
            bio?.let { updates["bio"] = it }
            if (updates.isNotEmpty()) {
                client.patch("/rest/v1/profiles") {
                    parameter("id", "eq.$userId")
                    setBody(updates)
                }
            }
        }
}
