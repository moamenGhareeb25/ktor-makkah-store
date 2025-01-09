package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.ByteArrayInputStream

object Firebase {
    fun init() {
        // Read Firebase credentials from environment variables
        val serviceAccountJson = """
        {
            "type": "${System.getenv("FIREBASE_TYPE")}",
            "project_id": "${System.getenv("FIREBASE_PROJECT_ID")}",
            "private_key_id": "${System.getenv("FIREBASE_PRIVATE_KEY_ID")}",
            "private_key": "${System.getenv("FIREBASE_PRIVATE_KEY")}",
            "client_email": "${System.getenv("FIREBASE_CLIENT_EMAIL")}",
            "client_id": "${System.getenv("FIREBASE_CLIENT_ID")}",
            "auth_uri": "${System.getenv("FIREBASE_AUTH_URI")}",
            "token_uri": "${System.getenv("FIREBASE_TOKEN_URI")}",
            "auth_provider_x509_cert_url": "${System.getenv("FIREBASE_AUTH_PROVIDER_CERT_URL")}",
            "client_x509_cert_url": "${System.getenv("FIREBASE_CLIENT_CERT_URL")}"
        }
        """.trimIndent()

        // Convert the JSON string to an InputStream
        val serviceAccountStream = ByteArrayInputStream(serviceAccountJson.toByteArray())

        // Initialize Firebase
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
            .build()

        FirebaseApp.initializeApp(options)
    }
}