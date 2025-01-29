package com.example.service

import com.example.model.Profile
import com.example.model.UpdateKey
import com.example.repository.ProfileRepository

/**
 * Handles pending updates for profiles, allowing updates to be reviewed and applied by authorized users.
 */
class PendingUpdateService(private val profileRepository: ProfileRepository) {

    /**
     * Saves updates to a profile as pending changes for review.
     * These updates will not be applied until reviewed and approved.
     */
    fun savePendingUpdate(profile: Profile) {
        val currentPending = profileRepository.fetchPendingUpdates(profile.userId).toMutableMap()
        profile.pendingUpdates.forEach { (key, value) ->
            currentPending[key] = value
        }
        profileRepository.savePendingUpdates(profile.userId, currentPending)
    }

    /**
     * Retrieves all pending updates for a specific profile.
     */
    fun getPendingUpdates(userId: String): Map<UpdateKey, String?> {
        return profileRepository.fetchPendingUpdates(userId)
    }

    /**
     * Applies pending updates to a profile after review and approval.
     */
    fun applyPendingUpdates(userId: String) {
        val pendingUpdates = getPendingUpdates(userId)
        profileRepository.applyPendingUpdates(userId, pendingUpdates)
    }

    /**
     * Clears all pending updates for a profile, typically after a rejection or application.
     */
    fun clearPendingUpdates(userId: String) {
        profileRepository.clearPendingUpdates(userId)
    }

}
