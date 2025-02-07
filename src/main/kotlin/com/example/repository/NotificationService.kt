package com.example.repository

import com.example.auth.FirebaseNotificationService
import com.example.model.NotificationType

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
        type: NotificationType
    ) {
        sendNotificationToDevice(recipientId, title, message, type )
    }

    /**
     * Notify the owner or delegated reviewer about an event.
     */
    suspend fun notifyOwnerOrReviewer(
        title: String,
        message: String,
        recipientId: String,
        type: NotificationType
    ) {
        sendNotificationToDevice(recipientId, title, message, type )
    }

    /**
     * Internal function to fetch the device token and send a notification.
     */
    private suspend fun sendNotificationToDevice(
        userId: String,
        title: String,
        message: String,
        type:NotificationType
    ) {
        val deviceToken = profileRepository.getDeviceToken(userId)
        if (deviceToken != null) {
            firebaseNotificationService.sendNotification(
                token = deviceToken,
                title = title,
                body = message,
                type = type
            )
        }
    }
}
