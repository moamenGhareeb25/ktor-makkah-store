package com.example.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.ByteArrayInputStream
import java.util.*

class FirebaseNotificationService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private suspend fun getAccessToken(): String {
        return withContext(Dispatchers.IO) {
            val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG is not set!")

            val firebaseConfigJson = String(Base64.getDecoder().decode(firebaseBase64)).trim()

            // 🔹 Convert JSON to ServiceAccountCredentials
            val credentials = ServiceAccountCredentials
                .fromStream(ByteArrayInputStream(firebaseConfigJson.toByteArray()))
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        }
    }

    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        sound: String = "default",
        data: Map<String, String> = emptyMap()
    ) {
        val accessToken = getAccessToken()

        // 🔹 Extract `project_id` from Firebase Config JSON
        val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
            ?: throw IllegalStateException("❌ FIREBASE_CONFIG is not set!")

        val firebaseConfigJson = String(Base64.getDecoder().decode(firebaseBase64)).trim()
        val projectId = Json.parseToJsonElement(firebaseConfigJson).jsonObject["project_id"]?.toString()?.replace("\"", "")
            ?: throw IllegalStateException("❌ Missing 'project_id' in Firebase Config!")

        val fcmUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        // 🔹 Construct FCM Payload (JSON)
        val payload = """
        {
          "message": {
            "token": "$token",
            "notification": {
              "title": "$title",
              "body": "$body",
              "sound": "$sound"
            },
            "data": ${Json.encodeToString(data)}
          }
        }
        """.trimIndent()

        try {
            val response: HttpResponse = client.post(fcmUrl) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(payload)
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
