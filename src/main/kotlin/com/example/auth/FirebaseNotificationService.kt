package com.example.auth

import com.example.firebase.FirebaseConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.*

class FirebaseNotificationService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true }) // ✅ Correct installation
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

        // 🔹 Retrieve Firebase Config from Render Environment
        val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
            ?: throw IllegalStateException("❌ FIREBASE_CONFIG not set")

        val decodedJson = try {
            String(Base64.getDecoder().decode(firebaseBase64))
        } catch (e: Exception) {
            throw IllegalStateException("❌ Error decoding FIREBASE_CONFIG: ${e.message}")
        }

        val firebaseConfig = try {
            Json.decodeFromString<FirebaseConfig>(decodedJson)
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

        try {
            val response: HttpResponse = client.post(fcmUrl) {
                header(HttpHeaders.Authorization, "key=$serverKey")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(Json.encodeToString(payload))
            }

            // 🔹 Print FCM Response Details
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
