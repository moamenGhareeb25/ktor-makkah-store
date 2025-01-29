package com.example.repository

import com.example.auth.sendNotification

class NotificationService(private val profileRepository: ProfileRepository) {

    /**
     * Notify a specific user.
     */
    fun notifyUser(title: String, message: String, recipientId: String, data: Map<String, String> = emptyMap()) {
        val deviceToken = getDeviceToken(recipientId)
        if (deviceToken != null) {
            sendNotification(deviceToken, title, message)
        }
    }


    /**
     * Notify the owner or delegated reviewer.
     */
    fun notifyOwnerOrReviewer(title: String, message: String, recipientId: String ,data: Map<String, String> = emptyMap()) {
        val deviceToken = getDeviceToken(recipientId)
        if (deviceToken != null) {
            sendNotification(deviceToken, title, message)
        }
    }

    /**
     * Helper function to fetch the device token from the database.
     */
    private fun getDeviceToken(userId: String): String? {
        return profileRepository.getDeviceToken(userId)
    }
}

