package com.example.repository

import com.example.auth.FirebaseNotificationService

class NotificationService(
    private val profileRepository: ProfileRepository,
    private val firebaseNotificationService: FirebaseNotificationService
) {

    /**
     * Send a notification to a specific user.
     * Includes dynamic sound, navigation, and dialog display.
     */
    suspend fun notifyUser(
        title: String,
        message: String,
        recipientId: String,
        data: Map<String, String> = emptyMap(),
        type: String
    ) {
        sendNotificationToDevice(recipientId, title, message, data, type )
    }

    /**
     * Notify the owner or delegated reviewer about an event.
     */
    suspend fun notifyOwnerOrReviewer(
        title: String,
        message: String,
        recipientId: String,
        data: Map<String, String> = emptyMap(),
        type: String
    ) {
        sendNotificationToDevice(recipientId, title, message, data, type )
    }

    /**
     * Internal function to fetch the device token and send a notification.
     */
    private suspend fun sendNotificationToDevice(
        userId: String,
        title: String,
        message: String,
        data: Map<String, String>,
        type:String
    ) {
        val deviceToken = profileRepository.getDeviceToken(userId)
        if (deviceToken != null) {
            firebaseNotificationService.sendNotification(
                token = deviceToken,
                title = title,
                body = message,
                data = data,
                type = type
            )
        }
    }
}
