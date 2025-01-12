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
    fun getProfile(userId: String): Profile? {
        return transaction {
            ProfileTable.select { ProfileTable.userId eq userId }
                .map { row ->
                    Profile(
                        userId = row[ProfileTable.userId],
                        name = row[ProfileTable.name],
                        email = row[ProfileTable.email],
                        personalNumber = row[ProfileTable.personalNumber],
                        workNumber = row[ProfileTable.workNumber],
                        profilePictureUrl = row[ProfileTable.profilePictureUrl],
                        createdAt = row[ProfileTable.createdAt]
                    )
                }.singleOrNull()
        }
    }

    fun updateProfile(profile: Profile) {
        transaction {
            ProfileTable.update({ ProfileTable.userId eq profile.userId }) {
                it[name] = profile.name
                it[email] = profile.email
                it[personalNumber] = profile.personalNumber
                it[workNumber] = profile.workNumber
                it[profilePictureUrl] = profile.profilePictureUrl
            }
        }
    }

    fun createProfile(profile: Profile) {
        transaction {
            ProfileTable.insert {
                it[userId] = profile.userId
                it[name] = profile.name
                it[email] = profile.email
                it[personalNumber] = profile.personalNumber
                it[workNumber] = profile.workNumber
                it[profilePictureUrl] = profile.profilePictureUrl
                it[createdAt] = profile.createdAt ?: System.currentTimeMillis()
            }
        }
    }

    fun deleteProfile(userId: String) {
        transaction {
            ProfileTable.deleteWhere { ProfileTable.userId eq userId }
        }
    }

    fun updateUserStatus(userId: String, online: Boolean) {
        transaction {
            ProfileTable.update({ ProfileTable.userId eq userId }) {
                it[ProfileTable.isOnline] = online
                it[ProfileTable.lastSeen] = if (online) null else System.currentTimeMillis()
            }
        }
    }

    fun getUserStatus(userId: String): Pair<Boolean, Long?> {
        return transaction {
            ProfileTable
                .select { ProfileTable.userId eq userId }
                .map { row -> row[ProfileTable.isOnline] to row[ProfileTable.lastSeen] }
                .singleOrNull() ?: (false to null)
        }
    }


    suspend fun updateAndBroadcastStatus(userId: String, online: Boolean) {
        updateUserStatus(userId, online)

        val statusUpdate = StatusUpdate(
            userId = userId,
            isOnline = online
        )

        // Broadcast via WebSocket
        activeConnections.forEach { (_, sessions) ->
            sessions.forEach { session ->
                session.send(Frame.Text(Json.encodeToString(StatusUpdate.serializer(), statusUpdate)))
            }
        }

        // Send FCM Notification
        val followers = getFollowers(userId)
        followers.forEach { followerId ->
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


    fun getAllUsers(): List<Profile> {
        return transaction {
            ProfileTable.selectAll()
                .map { row ->
                    Profile(
                        userId = row[ProfileTable.userId],
                        name = row[ProfileTable.name],
                        email = row[ProfileTable.email],
                        personalNumber = row[ProfileTable.personalNumber],
                        workNumber = row[ProfileTable.workNumber],
                        profilePictureUrl = row[ProfileTable.profilePictureUrl],
                        createdAt = row[ProfileTable.createdAt]
                    )
                }
        }
    }

    private fun getFollowers(userId: String): List<String> {
        return transaction {
            ChatParticipants
                .select { ChatParticipants.chatId inSubQuery
                        ChatParticipants.slice(ChatParticipants.chatId)
                            .select { ChatParticipants.userId eq userId }
                }
                .map { it[ChatParticipants.userId] }
                .distinct()
                .filter { it != userId } // Exclude the user themselves
        }
    }
    fun saveDeviceToken(userId: String, token: String) {
        transaction {
            // Remove existing tokens for the user to avoid duplicates
            DeviceTokens.deleteWhere { DeviceTokens.token eq token }

            // Insert the new token
            DeviceTokens.insert {
                it[DeviceTokens.userId] = userId
                it[DeviceTokens.token] = token
            }
        }
    }


    fun getDeviceToken(userId: String): String? {
        return transaction {
            DeviceTokens
                .select { DeviceTokens.userId eq userId }
                .map { it[DeviceTokens.token] }
                .singleOrNull()
        }
    }

}