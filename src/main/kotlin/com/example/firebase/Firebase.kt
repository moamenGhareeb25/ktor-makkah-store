package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.util.*

object Firebase {
    private var firebaseConfig: FirebaseConfig? = null

    @Synchronized
    fun init(): FirebaseConfig {
        if (firebaseConfig != null) return firebaseConfig!!

        try {
            // ✅ Get the base64-encoded JSON string
            val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG is not set!")

            println("🔍 Base64 Firebase Config:\n$firebaseBase64")

            // ✅ Decode the base64-encoded JSON string
            val firebaseConfigJson = String(Base64.getDecoder().decode(firebaseBase64)).trim()

            println("🔍 Decoded Firebase JSON:\n${firebaseConfigJson.take(500)}...")

            // ✅ Parse JSON into FirebaseConfig class
            val config = Json.decodeFromString<FirebaseConfig>(firebaseConfigJson)

            // ✅ Ensure proper private key formatting
            val formattedPrivateKey = config.privateKey
                .replace("\\\\n", "\n") // Fix escaped newlines
                .replace("\\r\\n", "\n") // Fix Windows-style newlines
                .replace("\r\n", "\n") // Extra safeguard
                .replace("\r", "\n") // Ensure all line breaks are `\n`
                .trim()

            // ✅ Manually construct JSON to ensure correctness
            val correctedJson = """
                {
                    "type": "service_account",
                    "project_id": "${config.projectId}",
                    "private_key_id": "${config.privateKeyId}",
                    "private_key": "${formattedPrivateKey}",
                    "client_email": "${config.clientEmail}",
                    "client_id": "${config.clientId}",
                    "auth_uri": "${config.authUri}",
                    "token_uri": "${config.tokenUri}",
                    "auth_provider_x509_cert_url": "${config.authProviderCertUrl}",
                    "client_x509_cert_url": "${config.clientCertUrl}"
                }
            """.trimIndent()

            println("✅ JSON Before Firebase Initialization:\n$correctedJson")

            // ✅ Convert JSON to InputStream with UTF-8 encoding
            val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(correctedJson.toByteArray(Charsets.UTF_8)))

            // ✅ Initialize Firebase
            if (FirebaseApp.getApps().isEmpty()) {
                val options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setDatabaseUrl(config.databaseUrl ?: throw IllegalStateException("❌ Database URL is missing!"))
                    .setStorageBucket(config.storageBucket)
                    .build()
                FirebaseApp.initializeApp(options)
                println("✅ Firebase initialized successfully.")
            }

            firebaseConfig = config
            return config

        } catch (e: Exception) {
            println("❌ Firebase Initialization Failed: ${e.message}")
            e.printStackTrace()
            throw IllegalStateException("❌ Firebase Initialization Failed: ${e.message}")
        }
    }
}


@Serializable
data class FirebaseConfig(
    @SerialName("type") val type: String = "service_account", // ✅ Always set to "service_account"
    @SerialName("project_id") val projectId: String,
    @SerialName("private_key_id") val privateKeyId: String,
    @SerialName("private_key") val privateKey: String,
    @SerialName("client_email") val clientEmail: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("auth_uri") val authUri: String,
    @SerialName("token_uri") val tokenUri: String,
    @SerialName("auth_provider_x509_cert_url") val authProviderCertUrl: String,
    @SerialName("client_x509_cert_url") val clientCertUrl: String,
    @SerialName("database_url") val databaseUrl: String? = null,
    @SerialName("storage_bucket") val storageBucket: String,
    @SerialName("auth_api_key") val authApiKey: String,
    @SerialName("messaging_sender_id") val messagingSenderId: String,
    @SerialName("app_id") val appId: String,
    @SerialName("measurement_id") val measurementId: String,
    @SerialName("fcm_server_key") val fcmServerKey: String,
    @SerialName("web_push_certificate_key") val webPushCertificateKey: String
)
