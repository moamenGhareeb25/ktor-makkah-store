package com.example.auth

import com.example.firebase.Firebase
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FirebaseNotificationService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true }) // ✅ Ensure proper JSON handling
        }
    }

    // 🔹 Firebase Config (Lazy Initialization)
    private val firebaseConfig by lazy {
        try {
            Firebase.init()
        } catch (e: Exception) {
            println("❌ Firebase initialization failed: ${e.message}")
            throw IllegalStateException("❌ Firebase not initialized properly. Cannot send notifications.")
        }
    }

    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        sound: String = "default",
        targetScreen: String = "",
        showDialog: Boolean = false,
        data: Map<String, String> = emptyMap()
    ) {
        val fcmUrl = "https://fcm.googleapis.com/fcm/send"

        // ✅ Retrieve & Validate FCM Server Key
        val serverKey = firebaseConfig.fcmServerKey.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("❌ Missing or empty 'fcmServerKey' in Firebase Config!")

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

        try {
            val response: HttpResponse = client.post(fcmUrl) {
                header(HttpHeaders.Authorization, "key=$serverKey")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(Json.encodeToString(JsonObject.serializer(), payload)) // ✅ Fixed serialization
            }

            val responseBody = response.bodyAsText()
            println("✅ FCM Response: ${response.status}")
            println("📜 Response Body: $responseBody")

            if (response.status != HttpStatusCode.OK) {
                throw IllegalStateException("❌ Failed to send FCM: ${response.status.value} - $responseBody")
            }

        } catch (e: Exception) {
            println("❌ Error sending FCM: ${e.message}")
            e.printStackTrace()
        }
    }
}
