package com.example.auth

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FirebaseNotificationService {
    private val client = HttpClient()

    suspend fun sendNotification(token: String, title: String, body: String, sound: String, targetScreen: String, showDialog: Boolean, data: Map<String, String>) {
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/makkah-store-operations/messages:send"
        val serverKey = System.getenv("FIREBASE_SERVER_KEY")
            ?: throw IllegalStateException("Missing FIREBASE_SERVER_KEY environment variable")

        val payload = buildJsonObject {
            put("message", buildJsonObject {
                put("token", token)
                put("notification", buildJsonObject {
                    put("title", title)
                    put("body", body)
                })
                put("data", buildJsonObject {
                    put("sound", sound)
                    put("targetScreen", targetScreen)
                    put("showDialog", showDialog)
                    data.forEach { (key, value) -> put(key, value) }
                })
            })
        }


        val response: HttpResponse = client.post(fcmUrl) {
            header(HttpHeaders.Authorization, "Bearer $serverKey")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(Json.encodeToString(payload))
        }

        println("✅ FCM Response: ${response.status}")
    }
}
