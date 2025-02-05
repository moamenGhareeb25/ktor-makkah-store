package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

object Firebase {
    private val json = Json {
        ignoreUnknownKeys = true  // ✅ Ignore extra fields
        isLenient = true          // ✅ Allow relaxed JSON syntax
        allowStructuredMapKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private var firebaseConfig: FirebaseConfig? = null

    fun init(): FirebaseConfig {
        if (firebaseConfig != null) return firebaseConfig!!  // ✅ Return cached config

        try {
            val firebaseConfigRaw = System.getenv("FIREBASE_CONFIG")
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG not set. Make sure it's added to Render.")

            val decodedJson = if (firebaseConfigRaw.startsWith("{")) {
                firebaseConfigRaw // ✅ JSON is already formatted correctly
            } else {
                String(Base64.getDecoder().decode(firebaseConfigRaw)) // ✅ Decode if Base64 encoded
            }

            println("🔍 Received JSON: $decodedJson")

            // ✅ Deserialize JSON **without modifying keys**
            firebaseConfig = json.decodeFromString<FirebaseConfig>(decodedJson)

            println("🔥 Firebase Configuration Loaded:")
            println("📌 Project ID: ${firebaseConfig!!.project_id}")
            println("📌 Database URL: ${firebaseConfig!!.database_url}")
            println("📌 Storage Bucket: ${firebaseConfig!!.storage_bucket}")

            val tempFile = File.createTempFile("firebase-admin", ".json").apply {
                writeText(decodedJson.replace("\\n", "\n"))
                deleteOnExit()
            }

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(tempFile.inputStream()))
                .setDatabaseUrl(firebaseConfig!!.database_url)
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("✅ Firebase initialized successfully!")
            } else {
                println("✅ Firebase already initialized.")
            }

            return firebaseConfig!!

        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Firebase Initialization Failed: ${e.message}")
            throw e
        }
    }
}


// 🔹 Firebase Configuration Data Class
@Serializable
data class FirebaseConfig(
    @SerialName("FIREBASE_PROJECT_ID") val project_id: String,
    @SerialName("FIREBASE_PRIVATE_KEY_ID") val private_key_id: String,
    @SerialName("FIREBASE_PRIVATE_KEY") val private_key: String,
    @SerialName("FIREBASE_CLIENT_EMAIL") val client_email: String,
    @SerialName("FIREBASE_CLIENT_ID") val client_id: String,
    @SerialName("FIREBASE_AUTH_URI") val auth_uri: String,
    @SerialName("FIREBASE_TOKEN_URI") val token_uri: String,
    @SerialName("FIREBASE_AUTH_PROVIDER_X509_CERT_URL") val auth_provider_x509_cert_url: String,
    @SerialName("FIREBASE_CLIENT_X509_CERT_URL") val client_x509_cert_url: String,
    @SerialName("FIREBASE_DATABASE_URL") val database_url: String,
    @SerialName("FIREBASE_STORAGE_BUCKET") val storage_bucket: String,
    @SerialName("FIREBASE_AUTH_API_KEY") val auth_api_key: String,
    @SerialName("FIREBASE_MESSAGING_SENDER_ID") val messaging_sender_id: String,
    @SerialName("FIREBASE_APP_ID") val app_id: String,
    @SerialName("FIREBASE_MEASUREMENT_ID") val measurement_id: String,
    @SerialName("FCM_SERVER_KEY") val fcm_server_key: String,
    @SerialName("WEB_PUSH_CERTIFICATE_KEY") val web_push_certificate_key: String
)

