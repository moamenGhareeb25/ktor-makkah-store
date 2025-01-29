package com.example.service

import com.example.repository.ProfileRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles user online/offline status updates.
 */
class UserStatusService(private val profileRepository: ProfileRepository) {
    private val mutex = Mutex()

    /**
     * Updates user status and ensures database consistency.
     */
    suspend fun updateUserStatus(userId: String, isOnline: Boolean) {
        mutex.withLock {
            profileRepository.updateOnlineStatus(userId, isOnline)
        }
    }
}