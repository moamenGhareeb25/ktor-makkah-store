package com.example.repository

import com.example.auth.sendNotification
import com.example.database.ChatParticipants
import com.example.database.DeviceTokens
import com.example.database.ProfileTable
import com.example.model.Profile
import com.example.model.StatusUpdate
import com.example.plugins.activeConnections
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ProfileRepository {

    // Retrieve a profile by user ID
    fun getProfile(userId: String): Profile? = transaction {
        ProfileTable.select { ProfileTable.userId eq userId }
            .map { row -> rowToProfile(row) }
            .singleOrNull()
    }

    // Create a new profile
    fun createProfile(profile: Profile) {
        transaction {
            ProfileTable.insert { row ->
                row[userId] = profile.userId
                row[name] = profile.name
                row[email] = profile.email
                row[personalNumber] = profile.personalNumber
                row[workNumber] = profile.workNumber
                row[profilePictureUrl] = profile.profilePictureUrl
                row[userRole] = profile.userRule
                row[createdAt] = profile.createdAt ?: System.currentTimeMillis()
            }
        }
        notifyManagerOrOwner(profile, "A new profile has been created.")
    }

    // Update an existing profile
    fun updateProfile(profile: Profile) {
        transaction {
            ProfileTable.update({ ProfileTable.userId eq profile.userId }) { row ->
                row[name] = profile.name
                row[email] = profile.email
                row[personalNumber] = profile.personalNumber
                row[workNumber] = profile.workNumber
                row[profilePictureUrl] = profile.profilePictureUrl
                row[userRole] = profile.userRule
            }
        }
        notifyManagerOrOwner(profile, "Profile updates are awaiting approval.")
    }

    // Notify manager or owner about profile changes
    private fun notifyManagerOrOwner(profile: Profile, message: String) {
        val managerId = getManagerOrOwnerId(profile.userRule)
        val deviceToken = getDeviceToken(managerId)

        if (deviceToken != null) {
            sendNotification(
                deviceToken = deviceToken,
                title = "Profile Notification",
                body = message,
                data = mapOf("profileId" to profile.userId)
            )
        }
    }

    // Get manager or owner ID based on user role
    private fun getManagerOrOwnerId(userRole: String?): String {
        return when (userRole) {
            "owner", "moderator" -> "manager-or-owner-id"
            else -> "default-manager-id"
        }
    }

    // Handle profile update decisions
    fun handleProfileUpdateDecision(profileId: String, decision: String, modifiedProfile: Profile? = null) {
        transaction {
            when (decision) {
                "ACCEPT" -> applyProfileChanges(profileId, modifiedProfile)
                "REJECT" -> println("Profile update for $profileId was rejected.")
                "MODIFY" -> applyProfileChanges(profileId, modifiedProfile)
                else -> throw IllegalArgumentException("Invalid decision: $decision")
            }
        }
    }

    // Apply changes to a profile
    private fun applyProfileChanges(profileId: String, modifiedProfile: Profile?) {
        if (modifiedProfile != null) {
            ProfileTable.update({ ProfileTable.userId eq profileId }) { row ->
                row[name] = modifiedProfile.name
                row[email] = modifiedProfile.email
                row[personalNumber] = modifiedProfile.personalNumber
                row[workNumber] = modifiedProfile.workNumber
                row[profilePictureUrl] = modifiedProfile.profilePictureUrl
                row[userRole] = modifiedProfile.userRule
            }
        }
    }

    // Delete a profile by user ID
    fun deleteProfile(userId: String) = transaction {
        ProfileTable.deleteWhere { ProfileTable.userId eq userId }
    }

    // Update user online status
    fun updateUserStatus(userId: String, online: Boolean) {
        transaction {
            ProfileTable.update({ ProfileTable.userId eq userId }) { row ->
                row[isOnline] = online
                row[lastSeen] = if (online) null else System.currentTimeMillis()
            }
        }
    }

    // Get user online status
    fun getUserStatus(userId: String): Pair<Boolean, Long?> = transaction {
        ProfileTable
            .select { ProfileTable.userId eq userId }
            .map { it[ProfileTable.isOnline] to it[ProfileTable.lastSeen] }
            .singleOrNull() ?: (false to null)
    }

    // Update and broadcast user status
    suspend fun updateAndBroadcastStatus(userId: String, online: Boolean) {
        updateUserStatus(userId, online)

        val statusUpdate = StatusUpdate(userId = userId, isOnline = online)

        // Broadcast via WebSocket
        activeConnections.forEach { (_, sessions) ->
            sessions.forEach { session ->
                session.send(Frame.Text(Json.encodeToString(StatusUpdate.serializer(), statusUpdate)))
            }
        }

        // Send FCM notifications to followers
        getFollowers(userId).forEach { followerId ->
            val deviceToken = getDeviceToken(followerId)
            if (deviceToken != null) {
                sendNotification(
                    deviceToken = deviceToken,
                    title = "Status Update",
                    body = "$userId is now ${if (online) "online" else "offline"}",
                    data = mapOf("userId" to userId, "isOnline" to online.toString())
                )
            }
        }
    }

    // Retrieve all users
    fun getAllUsers(): List<Profile> = transaction {
        ProfileTable.selectAll().map { row -> rowToProfile(row) }
    }

    // Get followers of a user
    private fun getFollowers(userId: String): List<String> = transaction {
        ChatParticipants
            .select { ChatParticipants.chatId inSubQuery ChatParticipants.slice(ChatParticipants.chatId).select { ChatParticipants.userId eq userId } }
            .map { it[ChatParticipants.userId] }
            .distinct()
            .filter { it != userId }
    }

    // Save device token for push notifications
    fun saveDeviceToken(userId: String, token: String) {
        transaction {
            DeviceTokens.deleteWhere { DeviceTokens.token eq token } // Prevent duplicates
            DeviceTokens.insert {
                it[DeviceTokens.userId] = userId
                it[DeviceTokens.token] = token
            }
        }
    }

    // Get device token by user ID
    fun getDeviceToken(userId: String): String? = transaction {
        DeviceTokens
            .select { DeviceTokens.userId eq userId }
            .map { it[DeviceTokens.token] }
            .singleOrNull()
    }

    // Map database row to Profile object
    private fun rowToProfile(row: ResultRow) = Profile(
        userId = row[ProfileTable.userId],
        name = row[ProfileTable.name],
        email = row[ProfileTable.email],
        personalNumber = row[ProfileTable.personalNumber],
        workNumber = row[ProfileTable.workNumber],
        profilePictureUrl = row[ProfileTable.profilePictureUrl],
        userRule = row[ProfileTable.userRole],
        createdAt = row[ProfileTable.createdAt]
    )
}
