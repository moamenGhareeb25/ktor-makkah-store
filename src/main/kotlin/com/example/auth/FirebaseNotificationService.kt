package com.example.auth

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.*

class FirebaseNotificationService {
    private val client = HttpClient()

    suspend fun sendNotification(token: String, title: String, body: String, sound: String, targetScreen: String, showDialog: Boolean, data: Map<String, String>) {
        val fcmUrl = "https://fcm.googleapis.com/fcm/send"

        // Retrieve Firebase Config from the environment
        val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
            ?: throw IllegalStateException("❌ FIREBASE_CONFIG not set")

        val decodedJson = String(Base64.getDecoder().decode(firebaseBase64))
        val firebaseConfig = Json.decodeFromString<FirebaseConfig>(decodedJson)

        val serverKey = firebaseConfig.fcm_server_key
            ?: throw IllegalStateException("❌ Missing FCM Server Key in Firebase Config")

        // Prepare Notification Payload
        val payload = buildJsonObject {
            put("to", token)
            put("notification", buildJsonObject {
                put("title", title)
                put("body", body)
                put("sound", sound)
                put("click_action", targetScreen)
            })
            put("data", buildJsonObject {
                put("showDialog", showDialog)
                data.forEach { (key, value) -> put(key, value) }
            })
        }

        // Send Notification Request
        val response: HttpResponse = client.post(fcmUrl) {
            header(HttpHeaders.Authorization, "key=$serverKey")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(Json.encodeToString(payload))
        }

        println("✅ FCM Response: ${response.status}")
    }
}

// 🔹 FirebaseConfig Data Class (if not imported already)
@kotlinx.serialization.Serializable
data class FirebaseConfig(
    val fcm_server_key: String,
    val databaseUrl: String,
    val storage_bucket: String,
    val auth_api_key: String
)
