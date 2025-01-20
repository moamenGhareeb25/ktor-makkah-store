package com.example.repository

import com.example.auth.sendNotification
import com.example.database.ChatParticipants
import com.example.database.ChatParticipants.joinedAt
import com.example.database.Chats
import com.example.database.Messages
import com.example.database.ProfileTable
import com.example.model.Chat
import com.example.model.Message
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ChatRepository(private val profileRepository: ProfileRepository) {

    /**
     * Create a private chat between two users.
     * @param user1 The first user ID.
     * @param user2 The second user ID.
     * @return The created Chat object.
     */
    fun createPrivateChat(user1: String, user2: String): Chat {
        val chatId = UUID.randomUUID().toString()
        return transaction {
            // Insert chat into Chats table
            Chats.insert {
                it[this.chatId] = chatId
                it[chatType] = "private"
                it[createdAt] = System.currentTimeMillis()
            }

            // Insert participants into ChatParticipants table
            ChatParticipants.batchInsert(listOf(user1, user2)) { user ->
                this[ChatParticipants.chatId] = chatId
                this[ChatParticipants.userId] = user
                this[joinedAt] = System.currentTimeMillis()
            }

            Chat(chatId, "private", System.currentTimeMillis())
        }
    }

    /**
     * Create a group chat with an admin and participants.
     * @param adminId The admin user ID.
     * @param groupName The name of the group.
     * @param participants The list of participant user IDs.
     * @return The created Chat object.
     */
    fun createGroupChat(adminId: String, groupName: String, participants: List<String>): Chat {
        val chatId = UUID.randomUUID().toString()
        return transaction {
            // Insert chat into Chats table
            Chats.insert {
                it[this.chatId] = chatId
                it[chatType] = "group"
                it[createdAt] = System.currentTimeMillis()
                it[Chats.groupName] = groupName
            }

            // Add admin to participants and insert into ChatParticipants table
            val allParticipants = (participants + adminId).distinct()
            ChatParticipants.batchInsert(allParticipants) { user ->
                this[ChatParticipants.chatId] = chatId
                this[ChatParticipants.userId] = user
                this[joinedAt] = System.currentTimeMillis()
            }

            Chat(chatId, "group", System.currentTimeMillis(), groupName)
        }
    }

    /**
     * Check if a user is part of a specific chat.
     * @param chatId The chat ID.
     * @param userId The user ID.
     * @return True if the user is in the chat, false otherwise.
     */
    fun isUserInChat(chatId: String, userId: String): Boolean {
        return transaction {
            ChatParticipants.select {
                ChatParticipants.chatId eq chatId and (ChatParticipants.userId eq userId)
            }.count() > 0
        }
    }

    /**
     * Get all participants of a chat.
     * @param chatId The chat ID.
     * @return A list of participant user IDs.
     */
    fun getParticipants(chatId: String): List<String> {
        return transaction {
            ChatParticipants.select { ChatParticipants.chatId eq chatId }
                .map { it[ChatParticipants.userId] }
        }
    }

    /**
     * Store a message in the database.
     * @param chatId The chat ID.
     * @param senderId The sender's user ID.
     * @param message The message object.
     * @return The generated message ID.
     */
    fun storeMessage(chatId: String, senderId: String, message: Message): String {
        val messageId = UUID.randomUUID().toString()
        transaction {
            Messages.insert {
                it[this.messageId] = messageId
                it[this.chatId] = chatId
                it[this.senderId] = senderId
                it[this.contentType] = message.contentType
                it[this.content] = message.content
                it[this.createdAt] = System.currentTimeMillis()
            }
        }
        return messageId
    }

    /**
     * Mark a message as read by a user.
     * @param messageId The message ID.
     * @param userId The user ID.
     * @return True if the message was marked as read, false otherwise.
     */
    fun markMessageAsRead(messageId: String, userId: String): Boolean {
        return transaction {
            Messages.update({ Messages.messageId eq messageId }) {
                it[isRead] = true
                it[readAt] = System.currentTimeMillis()
            } > 0
        }
    }

    /**
     * Retrieve messages from a chat with pagination.
     * @param chatId The chat ID.
     * @param limit The maximum number of messages to retrieve.
     * @param offset The offset for pagination.
     * @return A list of Message objects.
     */
    fun getMessages(chatId: String, limit: Int, offset: Int): List<Message> {
        return transaction {
            Messages.select { Messages.chatId eq chatId }
                .orderBy(Messages.createdAt, SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { row ->
                    Message(
                        senderId = row[Messages.senderId],
                        contentType = row[Messages.contentType],
                        content = row[Messages.content]
                    )
                }
        }
    }

    /**
     * Get all active chats for a user.
     * @param userId The user ID.
     * @return A list of Chat objects.
     */
    fun getActiveChats(userId: String): List<Chat> {
        return transaction {
            Chats.innerJoin(ChatParticipants)
                .select { ChatParticipants.userId eq userId }
                .map { row ->
                    Chat(
                        chatId = row[Chats.chatId],
                        chatType = row[Chats.chatType],
                        createdAt = row[Chats.createdAt],
                        groupName = row[Chats.groupName]
                    )
                }
        }
    }

    /**
     * Notify participants of a chat about a new message.
     * @param chatId The chat ID.
     * @param message The message object.
     * @param senderId The sender's user ID.
     */
    fun notifyParticipants(chatId: String, message: Message, senderId: String) {
        val participants = getParticipants(chatId).filter { it != senderId } // Exclude sender
        participants.forEach { userId ->
            val deviceToken = profileRepository.getDeviceToken(userId)
            if (deviceToken != null) {
                sendNotification(
                    deviceToken = deviceToken,
                    title = "New Message",
                    body = message.content,
                    data = mapOf("chatId" to chatId, "senderId" to senderId)
                )
            }
        }
    }

    fun getChatParticipants(chatId: String): List<String> {
        return transaction {
            ChatParticipants.select { ChatParticipants.chatId eq chatId }
                .map { it[ChatParticipants.userId] }
        }
    }
    fun getAllUsers(): List<String> {
        return transaction {
            ProfileTable.slice(ProfileTable.userId)
                .selectAll()
                .map { it[ProfileTable.userId] }
        }
    }
    fun getChatsWithDetails(userId: String): List<Map<String, Any?>> {
        return transaction {
            Chats.innerJoin(ChatParticipants)
                .select { ChatParticipants.userId eq userId }
                .map { row ->
                    val chatId = row[Chats.chatId]

                    // Get the last message for the chat
                    val lastMessageRow = Messages
                        .select { Messages.chatId eq chatId }
                        .orderBy(Messages.createdAt, SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()
                    val lastMessage = lastMessageRow?.get(Messages.content) ?: "No messages yet"

                    // Count unread messages for the chat
                    val unreadCount = Messages
                        .select { Messages.chatId eq chatId and (Messages.isRead eq false) }
                        .count()

                    mapOf(
                        "chatId" to chatId,
                        "chatType" to row[Chats.chatType],
                        "createdAt" to row[Chats.createdAt],
                        "groupName" to (row[Chats.groupName] ?: "Untitled Group"),
                        "lastMessage" to lastMessage,
                        "unreadCount" to unreadCount
                    )
                }
        }
    }

}
