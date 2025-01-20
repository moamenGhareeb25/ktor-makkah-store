package com.example.repository

import com.example.database.DeviceTokens
import com.example.database.ProfileTable
import com.example.model.Profile
import com.example.model.UpdateKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction

class ProfileRepository {

    /**
     * Retrieve a profile by user ID.
     */
    fun getProfile(userId: String): Profile? = transaction {
        ProfileTable.select { ProfileTable.userId eq userId }
            .map(::rowToProfile)
            .singleOrNull()
    }

    /**
     * Create a new profile.
     * Only the owner or "Moamen" can create a profile.
     */
    fun createProfile(profile: Profile, requesterId: String) {
        require(isAuthorized(requesterId, profile.userId)) {
            "Permission denied: Only the owner or Moamen can create this profile."
        }
        transaction {
            ProfileTable.insert { it.mapProfile(profile) }
        }
    }

    /**
     * Update an existing profile.
     * If the requester is not authorized, save updates in pendingUpdates.
     */
    fun updateProfile(profile: Profile, requesterId: String) {
        if (!isAuthorized(requesterId, profile.userId)) {
            savePendingUpdates(profile)
        } else {
            transaction { ProfileTable.update({ ProfileTable.userId eq profile.userId }) { it.mapProfile(profile) } }
        }
    }

    /**
     * Review pending updates for a profile.
     */
    fun reviewPendingUpdates(profileId: String, decision: String, requesterId: String, modifiedProfile: Profile? = null) {
        val profile = getProfile(profileId) ?: throw IllegalArgumentException("Profile not found")
        require(isOwner(requesterId, profile.userId)) { "Permission denied: Only the owner can review pending updates." }

        transaction {
            when (decision.uppercase()) {
                "ACCEPT" -> applyPendingUpdates(profileId)
                "REJECT" -> clearPendingUpdates(profileId)
                "MODIFY" -> {
                    if (modifiedProfile != null) {
                        ProfileTable.update({ ProfileTable.userId eq profile.userId }) { it.mapProfile(modifiedProfile) }
                    } else throw IllegalArgumentException("Modified profile must be provided.")
                }
                else -> throw IllegalArgumentException("Invalid decision: $decision")
            }
        }
    }

    /**
     * Apply pending updates to a profile.
     */
    private fun applyPendingUpdates(profileId: String) {
        val pendingUpdates = fetchPendingUpdates(profileId)
        ProfileTable.update({ ProfileTable.userId eq profileId }) {
            pendingUpdates.forEach { (key, value) ->
                it[key.getColumn()] = value
            }
            it[ProfileTable.pendingUpdates] = null // Clear pending updates
        }
    }

    /**
     * Clear pending updates for a profile.
     */
    private fun clearPendingUpdates(profileId: String) {
        ProfileTable.update({ ProfileTable.userId eq profileId }) {
            it[ProfileTable.pendingUpdates] = null
        }
    }

    /**
     * Save pending updates for later review.
     */
    private fun savePendingUpdates(profile: Profile) {
        val currentPending = fetchPendingUpdates(profile.userId)
        profile.pendingUpdates.forEach { (key, value) -> currentPending[key] = value }
        ProfileTable.update({ ProfileTable.userId eq profile.userId }) {
            it[pendingUpdates] = serializePendingUpdates(currentPending)
        }
    }

    /**
     * Check if the requester is authorized.
     */
    private fun isAuthorized(requesterId: String, targetUserId: String): Boolean {
        return requesterId == targetUserId || requesterId.equals("Moamen", ignoreCase = true)
    }

    /**
     * Check if the requester is the owner.
     */
    private fun isOwner(requesterId: String, targetUserId: String): Boolean {
        return requesterId == targetUserId
    }

    /**
     * Fetch pending updates for a profile.
     */
    private fun fetchPendingUpdates(profileId: String): MutableMap<UpdateKey, String?> {
        val json = ProfileTable
            .select { ProfileTable.userId eq profileId }
            .map { it[ProfileTable.pendingUpdates] }
            .singleOrNull()
        return if (json.isNullOrEmpty()) mutableMapOf() else deserializePendingUpdates(json)
    }

    /**
     * Serialize pending updates to JSON.
     */
    private fun serializePendingUpdates(updates: Map<UpdateKey, String?>): String = Json.encodeToString(updates)

    /**
     * Deserialize pending updates from JSON.
     */
    private fun deserializePendingUpdates(data: String): MutableMap<UpdateKey, String?> =
        Json.decodeFromString<Map<UpdateKey, String?>>(data).toMutableMap()

    /**
     * Map a database row to a Profile object.
     */
    private fun rowToProfile(row: ResultRow): Profile = Profile(
        userId = row[ProfileTable.userId],
        name = row[ProfileTable.name],
        email = row[ProfileTable.email],
        personalNumber = row[ProfileTable.personalNumber],
        workNumber = row[ProfileTable.workNumber],
        profilePictureUrl = row[ProfileTable.profilePictureUrl],
        userRule = row[ProfileTable.userRole],
        createdAt = row[ProfileTable.createdAt],
        nickname = row[ProfileTable.nickname],
        isOnline = row[ProfileTable.isOnline],
        lastSeen = row[ProfileTable.lastSeen],
        pendingUpdates = deserializePendingUpdates(row[ProfileTable.pendingUpdates] ?: "{}")
    )

    /**
     * Extension function to map a Profile object to a database statement.
     */
    private fun UpdateStatement.mapProfile(profile: Profile) {
        this[ProfileTable.name] = profile.name
        this[ProfileTable.email] = profile.email
        this[ProfileTable.personalNumber] = profile.personalNumber
        this[ProfileTable.workNumber] = profile.workNumber
        this[ProfileTable.profilePictureUrl] = profile.profilePictureUrl
        this[ProfileTable.userRole] = profile.userRule
        this[ProfileTable.nickname] = profile.nickname
        this[ProfileTable.isOnline] = profile.isOnline
        this[ProfileTable.lastSeen] = profile.lastSeen
        this[ProfileTable.pendingUpdates] = serializePendingUpdates(profile.pendingUpdates)
    }

    private fun InsertStatement<Number>.mapProfile(profile: Profile) {
        this[ProfileTable.userId] = profile.userId
        this[ProfileTable.createdAt] = profile.createdAt ?: System.currentTimeMillis()
        mapProfile(profile)
    }

    /**
     * Extension function for mapping UpdateKey to database columns.
     */
    private fun UpdateKey.getColumn(): Column<String?> = when (this) {
        UpdateKey.NAME -> ProfileTable.name
        UpdateKey.EMAIL -> ProfileTable.email
        UpdateKey.NICKNAME -> ProfileTable.nickname
        UpdateKey.PERSONAL_NUMBER -> ProfileTable.personalNumber
        UpdateKey.WORK_NUMBER -> ProfileTable.workNumber
        UpdateKey.PROFILE_PICTURE_URL -> ProfileTable.profilePictureUrl
        UpdateKey.USER_ROLE -> ProfileTable.userRole
    }
    /**
     * Get the online status and last seen of a user.
     */
    fun getUserStatus(userId: String): Pair<Boolean, Long?> {
        return transaction {
            ProfileTable.select { ProfileTable.userId eq userId }
                .map { it[ProfileTable.isOnline] to it[ProfileTable.lastSeen] }
                .singleOrNull() ?: (false to null) // Default to offline and no last seen
        }
    }

    /**
     * Delete a profile by user ID.
     */
    fun deleteProfile(userId: String) {
        transaction {
            ProfileTable.deleteWhere { ProfileTable.userId eq userId }
        }
    }

    /**
     * Update a user's online status and broadcast the status change.
     */
    suspend fun updateAndBroadcastStatus(userId: String, online: Boolean) {
        // Update the user's online status in the database
        transaction {
            ProfileTable.update({ ProfileTable.userId eq userId }) {
                it[isOnline] = online
                it[lastSeen] = if (online) null else System.currentTimeMillis()
            }
        }

        // Broadcast the status change to active WebSocket connections
        broadcastStatusUpdate(userId, online)
    }

    /**
     * Broadcast the user's status change to active WebSocket connections.
     */
    private suspend fun broadcastStatusUpdate(userId: String, online: Boolean) {
        val statusUpdate = mapOf(
            "userId" to userId,
            "isOnline" to online
        )

        val statusUpdateJson = Json.encodeToString(statusUpdate)

        // Simulate broadcasting to WebSocket connections
        withContext(Dispatchers.IO) {
            ProfileWebSocketManager.broadcast(statusUpdateJson)
        }
    }
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
     * Retrieve the device token for a user.
     * @param userId The user ID.
     * @return The device token if it exists, null otherwise.
     */
    fun getDeviceToken(userId: String): String? {
        return transaction {
            DeviceTokens.select { DeviceTokens.userId eq userId }
                .map { it[DeviceTokens.token] }
                .singleOrNull()
        }
    }

    suspend fun updateLastSeen(userId: String) {
        transaction {
            ProfileTable.update({ ProfileTable.userId eq userId }) {
                it[lastSeen] = System.currentTimeMillis()
            }
        }
    }

}

