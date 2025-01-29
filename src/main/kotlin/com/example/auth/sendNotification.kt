package com.example.auth

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.Message as FCMMessage

fun sendNotification(deviceToken: String, title: String, body: String, data: Map<String, String> = emptyMap()) {
    try {
        val message = FCMMessage.builder()
            .setToken(deviceToken)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .putAllData(data)  // <-- Ensure extra data is included
            .build()

        FirebaseMessaging.getInstance().send(message)
    } catch (e: Exception) {
        println("❌ Failed to send notification: ${e.message}")
    }
}
