package com.example

import android.content.Context
import com.example.R
import com.google.auth.oauth2.GoogleCredentials
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.InputStream

object FcmHttpV1Manager {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                encodeDefaults = true
            })
        }
    }

    private var accessToken: String? = null
    private var tokenExpiry: Long = 0

    private suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return@withContext accessToken
        }

        try {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.service_account)
            val credentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            credentials.refreshIfExpired()
            accessToken = credentials.accessToken.tokenValue
            tokenExpiry = credentials.accessToken.expirationTime.time - 60000 // Buffer
            accessToken
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun sendNotification(
        context: Context,
        token: String? = null,
        topic: String? = null,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        val auth = getAccessToken(context) ?: return
        val projectId = "chat-4e1d0" 

        try {
            client.post("https://fcm.googleapis.com/v1/projects/$projectId/messages:send") {
                header(HttpHeaders.Authorization, "Bearer $auth")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("message", buildJsonObject {
                        if (token != null) {
                            put("token", token)
                        } else if (topic != null) {
                            put("topic", topic)
                        }
                        
                        put("notification", buildJsonObject {
                            put("title", title)
                            put("body", body)
                        })
                        
                        put("data", buildJsonObject {
                            data.forEach { (k, v) -> put(k, v) }
                            put("title", title)
                            put("message", body)
                        })
                        
                        put("android", buildJsonObject {
                            put("priority", "high")
                        })
                    })
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
