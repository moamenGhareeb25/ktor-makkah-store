package com.example.auth

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message as FCMMessage

fun sendNotification(deviceToken: String, title: String, body: String) {
    try {
        val message = FCMMessage.builder()
            .setToken(deviceToken)
            .setNotification(
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .build()

        FirebaseMessaging.getInstance().send(message)
    } catch (e: Exception) {
        println("Failed to send notification: ${e.message}")
    }
}
