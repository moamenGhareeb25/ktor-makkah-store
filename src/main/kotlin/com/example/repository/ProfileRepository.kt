package com.example.repository

import com.example.database.DeviceTokens
import com.example.database.ProfileTable
import com.example.model.Profile
import com.example.model.UpdateKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Handles database operations for profiles.
 */
class ProfileRepository {
    private val mutex = Mutex()

    /**
     * Retrieves a profile by user ID.
     */
    fun getProfile(userId: String): Profile? {
        return transaction {
            ProfileTable.select { ProfileTable.userId eq userId }
                .map(::rowToProfile)
                .singleOrNull()
        }
    }

    /**
     * Retrieves all profiles.
     */
    fun getAllProfiles(): List<Profile> {
        return transaction {
            ProfileTable.selectAll().map(::rowToProfile)
        }
    }

    /**
     * Creates a new profile.
     */
    fun createProfile(profile: Profile) {
        transaction {
            ProfileTable.insert {
                it[userId] = profile.userId
                it[name] = profile.name
                it[email] = profile.email
                it[personalNumber] = profile.personalNumber
                it[workNumber] = profile.workNumber
                it[profilePictureUrl] = profile.profilePictureUrl
                it[userRole] = profile.userRole
                it[nickname] = profile.nickname
                it[isOnline] = profile.isOnline
                it[lastSeen] = profile.lastSeen
                it[createdAt] = profile.createdAt ?: System.currentTimeMillis()
            }
        }
    }

    /**
     * Updates an existing profile.
     */
    fun updateProfile(profile: Profile) {
        transaction {
            ProfileTable.update({ ProfileTable.userId eq profile.userId }) {
                it[name] = profile.name
                it[email] = profile.email
                it[personalNumber] = profile.personalNumber
                it[workNumber] = profile.workNumber
                it[profilePictureUrl] = profile.profilePictureUrl
                it[userRole] = profile.userRole
                it[nickname] = profile.nickname
                it[isOnline] = profile.isOnline
                it[lastSeen] = profile.lastSeen
            }
        }
    }

    /**
     * Deletes a profile by user ID.
     */
    fun deleteProfile(userId: String) {
        transaction {
            ProfileTable.deleteWhere { ProfileTable.userId eq userId }
        }
    }

    /**
     * Fetches pending updates for a specific profile.
     */
    fun fetchPendingUpdates(userId: String): Map<UpdateKey, String?> {
        val pendingUpdatesJson = transaction {
            ProfileTable.select { ProfileTable.userId eq userId }
                .map { it[ProfileTable.pendingUpdates] }
                .singleOrNull()
        }
        return if (!pendingUpdatesJson.isNullOrEmpty()) {
            Json.decodeFromString(pendingUpdatesJson)
        } else {
            emptyMap()
        }
    }

    /**
     * Saves pending updates for a profile.
     */
    fun savePendingUpdates(userId: String, updates: Map<UpdateKey, String?>) {
        val serializedUpdates = Json.encodeToString(updates)
        transaction {
            ProfileTable.update({ ProfileTable.userId eq userId }) {
                it[ProfileTable.pendingUpdates] = serializedUpdates
            }
        }
    }

    /**
     * Clears pending updates for a profile.
     */
    fun clearPendingUpdates(userId: String) {
        transaction {
            ProfileTable.update({ ProfileTable.userId eq userId }) {
                it[ProfileTable.pendingUpdates] = null
            }
        }
    }


    /**
     * Maps a database row to a Profile object.
     */
    private fun rowToProfile(row: ResultRow): Profile {
        return Profile(
            userId = row[ProfileTable.userId],
            name = row[ProfileTable.name],
            email = row[ProfileTable.email],
            personalNumber = row[ProfileTable.personalNumber],
            workNumber = row[ProfileTable.workNumber],
            profilePictureUrl = row[ProfileTable.profilePictureUrl],
            userRole = row[ProfileTable.userRole],
            nickname = row[ProfileTable.nickname],
            isOnline = row[ProfileTable.isOnline],
            lastSeen = row[ProfileTable.lastSeen],
            createdAt = row[ProfileTable.createdAt],
            pendingUpdates = row[ProfileTable.pendingUpdates]
                ?.let { deserializePendingUpdates(it).toMutableMap() }
                ?: mutableMapOf()
        )
    }


    /**
     * Deserializes pending updates from JSON.
     */
    private fun deserializePendingUpdates(updatesJson: String): Map<UpdateKey, String?> {
        return Json.decodeFromString(updatesJson)
    }

    fun applyPendingUpdates(userId: String, updates: Map<UpdateKey, String?>) {
        transaction {
            ProfileTable.update({ ProfileTable.userId eq userId }) {
                updates.forEach { (key, value) ->
                    it[key.getColumn()] = value
                }
                it[ProfileTable.pendingUpdates] = null // Clear pending updates
            }
        }
    }

    /**
     * Updates the online status of a user in the database and broadcasts the change via WebSocket.
     * @param userId The ID of the user.
     * @param isOnline Whether the user is online or offline.
     */
    suspend fun updateAndBroadcastStatus(userId: String, isOnline: Boolean) {
        // Update the user's status in the database
        transaction {
            ProfileTable.update({ ProfileTable.userId eq userId }) {
                it[ProfileTable.isOnline] = isOnline
                it[ProfileTable.lastSeen] = if (isOnline) null else System.currentTimeMillis()
            }
        }

        // Broadcast the status change to connected clients
        broadcastStatusChange(userId, isOnline)
    }

    /**
     * Broadcasts the online/offline status change via WebSocket.
     * @param userId The ID of the user.
     * @param isOnline Whether the user is online or offline.
     */
    private suspend fun broadcastStatusChange(userId: String, isOnline: Boolean) {
        val statusUpdate = mapOf(
            "userId" to userId,
            "isOnline" to isOnline
        )
        val statusUpdateJson = Json.encodeToString(statusUpdate)

        // Send the update over WebSocket
        ProfileWebSocketManager.broadcast(statusUpdateJson)
    }

    /**
     * Saves a device token for the given user.
     * Ensures there are no duplicate tokens.
     */
    fun saveDeviceToken(userId: String, token: String) {
        transaction {
            // Remove any duplicate tokens to avoid conflicts
            DeviceTokens.deleteWhere { DeviceTokens.token eq token }

            // Insert the new token for the user
            DeviceTokens.insert {
                it[DeviceTokens.userId] = userId
                it[DeviceTokens.token] = token
            }
        }
    }

    /**
     * Retrieves the most recent device token for a user.
     * @param userId The user ID.
     * @return The latest device token if available, otherwise null.
     */
    fun getDeviceToken(userId: String): String? {
        return transaction {
            DeviceTokens
                .select { DeviceTokens.userId eq userId }
                .map { it[DeviceTokens.token] }
                .singleOrNull() // Return only one token per user
        }
    }

    /**
     * Deletes a device token when a user logs out.
     * @param userId The user ID whose token should be removed.
     */
    fun deleteDeviceToken(userId: String) {
        transaction {
            DeviceTokens.deleteWhere { DeviceTokens.userId eq userId }
        }
    }

    /**
     * Fetch all profiles that have pending updates for review.
     */
    fun getAllPendingProfiles(): List<Profile> {
        return transaction {
            ProfileTable.select { ProfileTable.pendingUpdates.isNotNull() }
                .map(::rowToProfile)
        }
    }


    /**
     * Updates a user's online status in the database.
     */
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean) {
        mutex.withLock {
            transaction {
                ProfileTable.update({ ProfileTable.userId eq userId }) {
                    it[ProfileTable.isOnline] = isOnline
                    it[ProfileTable.lastSeen] = if (isOnline) null else System.currentTimeMillis()
                }
            }
        }
    }

    /**
     * Updates last seen timestamp when a user disconnects.
     */
    suspend fun updateLastSeen(userId: String) {
        mutex.withLock {
            transaction {
                ProfileTable.update({ ProfileTable.userId eq userId }) {
                    it[ProfileTable.lastSeen] = System.currentTimeMillis()
                }
            }
        }
    }

    /**
     * Get online status of a user.
     */
    fun isUserOnline(userId: String): Boolean {
        return transaction {
            ProfileTable.select { ProfileTable.userId eq userId }
                .map { it[ProfileTable.isOnline] }
                .firstOrNull() ?: false
        }
    }
}


