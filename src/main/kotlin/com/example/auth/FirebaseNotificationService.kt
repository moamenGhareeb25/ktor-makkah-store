package com.example.auth

import com.example.firebase.FirebaseConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.*

class FirebaseNotificationService {
    private val client = HttpClient()

    companion object {
        private val json = Json { ignoreUnknownKeys = true } // 🔹 Static instance for performance
    }

    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        sound: String,
        targetScreen: String,
        showDialog: Boolean,
        data: Map<String, String>
    ) {
        val fcmUrl = "https://fcm.googleapis.com/fcm/send"

        // 🔹 Retrieve Firebase Config from Render Environment
        val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
            ?: throw IllegalStateException("❌ FIREBASE_CONFIG not set")

        val decodedJson = try {
            String(Base64.getDecoder().decode(firebaseBase64))
        } catch (e: Exception) {
            throw IllegalStateException("❌ Error decoding FIREBASE_CONFIG: ${e.message}")
        }

        val firebaseConfig = try {
            json.decodeFromString<FirebaseConfig>(decodedJson)
        } catch (e: Exception) {
            throw IllegalStateException("❌ Error parsing FirebaseConfig JSON: ${e.message}")
        }

        val serverKey = firebaseConfig.fcm_server_key
            ?: throw IllegalStateException("❌ Missing FCM Server Key in Firebase Config")

        // 🔹 Construct Notification Payload
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

        // 🔹 Convert payload to JSON string correctly
        val jsonPayload = json.encodeToString(JsonObject.serializer(), payload)

        // 🔹 Send FCM Notification
        val response: HttpResponse = client.post(fcmUrl) {
            header(HttpHeaders.Authorization, "key=$serverKey")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(jsonPayload) // 🔹 Corrected serialization
        }

        println("✅ FCM Response: ${response.status}")
    }
}
