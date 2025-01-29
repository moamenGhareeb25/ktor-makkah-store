package com.example.repository

import com.example.auth.FirebaseNotificationService
import com.example.database.ChatParticipants
import com.example.database.Chats
import com.example.database.Messages
import com.example.database.ProfileTable
import com.example.model.Chat
import com.example.model.Message
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ChatRepository(
    private val profileRepository: ProfileRepository,
    private val firebaseNotificationService: FirebaseNotificationService
) {

    /**
     * Create a private chat between two users.
     */
    fun createPrivateChat(user1: String, user2: String): Chat {
        val chatId = UUID.randomUUID().toString()
        return transaction {
            Chats.insert {
                it[this.chatId] = chatId
                it[chatType] = "private"
                it[createdAt] = System.currentTimeMillis()
            }

            ChatParticipants.batchInsert(listOf(user1, user2)) { user ->
                this[ChatParticipants.chatId] = chatId
                this[ChatParticipants.userId] = user
                this[ChatParticipants.joinedAt] = System.currentTimeMillis()
            }

            Chat(chatId, "private", System.currentTimeMillis())
        }
    }

    /**
     * Create a group chat.
     */
    fun createGroupChat(adminId: String, groupName: String, participants: List<String>): Chat {
        val chatId = UUID.randomUUID().toString()
        return transaction {
            Chats.insert {
                it[this.chatId] = chatId
                it[chatType] = "group"
                it[createdAt] = System.currentTimeMillis()
                it[Chats.groupName] = groupName
            }

            val allParticipants = (participants + adminId).distinct()
            ChatParticipants.batchInsert(allParticipants) { user ->
                this[ChatParticipants.chatId] = chatId
                this[ChatParticipants.userId] = user
                this[ChatParticipants.joinedAt] = System.currentTimeMillis()
            }

            Chat(chatId, "group", System.currentTimeMillis(), groupName)
        }
    }

    /**
     * Store a message and notify participants.
     */
    suspend fun storeMessage(chatId: String, senderId: String, message: Message): String {
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
        notifyParticipants(chatId, message, senderId)
        return messageId
    }

    /**
     * Notify participants of a chat about a new message.
     */
    suspend fun notifyParticipants(chatId: String, message: Message, senderId: String) {
        val participants = getParticipants(chatId).filter { it != senderId }

        participants.forEach { userId ->
            val deviceToken = profileRepository.getDeviceToken(userId)
            if (deviceToken != null) {
                firebaseNotificationService.sendNotification(
                    token = deviceToken,
                    title = "New Message",
                    body = message.content,
                    sound = "message_tone",
                    targetScreen = "ChatActivity",
                    showDialog = false,
                    data = mapOf("chatId" to chatId, "senderId" to senderId)
                )
            }
        }
    }

    /**
     * Get participants of a chat.
     */
    fun getParticipants(chatId: String): List<String> {
        return transaction {
            ChatParticipants.select { ChatParticipants.chatId eq chatId }
                .map { it[ChatParticipants.userId] }
        }
    }

    /**
     * Get all active chats for a user.
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
     * Get chat details with last message & unread count.
     */
    fun getChatsWithDetails(userId: String): List<Map<String, Any?>> {
        return transaction {
            Chats.innerJoin(ChatParticipants)
                .select { ChatParticipants.userId eq userId }
                .map { row ->
                    val chatId = row[Chats.chatId]

                    val lastMessageRow = Messages
                        .select { Messages.chatId eq chatId }
                        .orderBy(Messages.createdAt, SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()
                    val lastMessage = lastMessageRow?.get(Messages.content) ?: "No messages yet"

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

    fun getAllUsers(): List<String> {
        return transaction {
            ProfileTable.slice(ProfileTable.userId)
                .selectAll()
                .map { it[ProfileTable.userId] }
        }
    }
}
