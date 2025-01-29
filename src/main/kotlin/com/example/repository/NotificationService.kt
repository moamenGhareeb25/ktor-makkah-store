package com.example.repository

import com.example.auth.sendNotification

class NotificationService(private val profileRepository: ProfileRepository) {

    /**
     * Send a notification to a specific user.
     */
    fun notifyUser(title: String, message: String, recipientId: String, data: Map<String, String> = emptyMap()) {
        sendNotificationToDevice(recipientId, title, message, data)
    }

    /**
     * Notify the owner or delegated reviewer about an event.
     */
    fun notifyOwnerOrReviewer(title: String, message: String, recipientId: String, data: Map<String, String> = emptyMap()) {
        sendNotificationToDevice(recipientId, title, message, data)
    }

    /**
     * Internal function to fetch the device token and send a notification.
     */
    private fun sendNotificationToDevice(userId: String, title: String, message: String, data: Map<String, String>) {
        val deviceToken = profileRepository.getDeviceToken(userId)
        if (deviceToken != null) {
            sendNotification(deviceToken, title, message, data) // ✅ Passes `data` properly
        }
    }
}
