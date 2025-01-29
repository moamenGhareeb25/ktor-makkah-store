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
        sound: String = "default_notification",
        targetScreen: String = "MainActivity",
        showDialog: Boolean = false,
        data: Map<String, String> = emptyMap()
    ) {
        sendNotificationToDevice(recipientId, title, message, sound, targetScreen, showDialog, data)
    }

    /**
     * Notify the owner or delegated reviewer about an event.
     */
    suspend fun notifyOwnerOrReviewer(
        title: String,
        message: String,
        recipientId: String,
        sound: String = "alert_sound",
        targetScreen: String = "ReviewActivity",
        showDialog: Boolean = true,
        data: Map<String, String> = emptyMap()
    ) {
        sendNotificationToDevice(recipientId, title, message, sound, targetScreen, showDialog, data)
    }

    /**
     * Internal function to fetch the device token and send a notification.
     */
    private suspend fun sendNotificationToDevice(
        userId: String,
        title: String,
        message: String,
        sound: String,
        targetScreen: String,
        showDialog: Boolean,
        data: Map<String, String>
    ) {
        val deviceToken = profileRepository.getDeviceToken(userId)
        if (deviceToken != null) {
            firebaseNotificationService.sendNotification(
                token = deviceToken,
                title = title,
                body = message,
                sound = sound,
                targetScreen = targetScreen,
                showDialog = showDialog,
                data = data
            )
        }
    }
}
