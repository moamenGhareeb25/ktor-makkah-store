package com.example.repository

import com.example.auth.sendNotification
import com.example.database.ChatParticipants
import com.example.database.Chats
import com.example.database.Messages
import com.example.database.ProfileTable
import com.example.model.Chat
import com.example.model.Message
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ChatRepository(private val profileRepository: ProfileRepository) {

    fun createPrivateChat(user1: String, user2: String): Chat {
        val chatId = UUID.randomUUID().toString()
        return transaction {
            try {
                // Insert chat into Chats table
                Chats.insert {
                    it[Chats.chatId] = chatId
                    it[Chats.chatType] = "private"
                    it[Chats.createdAt] = System.currentTimeMillis()
                }

                // Insert participants into ChatParticipants table
                ChatParticipants.batchInsert(listOf(user1, user2)) { user ->
                    this[ChatParticipants.chatId] = chatId
                    this[ChatParticipants.userId] = user
                    this[ChatParticipants.joinedAt] = System.currentTimeMillis()
                }

                Chat(chatId, "private", System.currentTimeMillis())
            } catch (e: Exception) {
                e.printStackTrace()
                throw IllegalStateException("Failed to create private chat: ${e.message}")
            }
        }
    }

    fun createGroupChat(adminId: String, groupName: String, participants: List<String>): Chat {
        val chatId = UUID.randomUUID().toString()
        return transaction {
            try {
                // Insert chat into Chats table
                Chats.insert {
                    it[Chats.chatId] = chatId
                    it[Chats.chatType] = "group"
                    it[Chats.createdAt] = System.currentTimeMillis()
                }

                // Add admin to participants and insert into ChatParticipants table
                val allParticipants = (participants + adminId).distinct()
                ChatParticipants.batchInsert(allParticipants) { user ->
                    this[ChatParticipants.chatId] = chatId
                    this[ChatParticipants.userId] = user
                    this[ChatParticipants.joinedAt] = System.currentTimeMillis()
                }

                Chat(chatId, "group", System.currentTimeMillis(), groupName)
            } catch (e: Exception) {
                e.printStackTrace()
                throw IllegalStateException("Failed to create group chat: ${e.message}")
            }
        }
    }


    fun isUserInChat(chatId: String, userId: String): Boolean {
        return transaction {
            ChatParticipants.select {
                ChatParticipants.chatId eq chatId and (ChatParticipants.userId eq userId)
            }.count() > 0
        }
    }

    fun getParticipants(chatId: String): List<String> {
        return transaction {
            ChatParticipants.select { ChatParticipants.chatId eq chatId }
                .map { it[ChatParticipants.userId] }
        }
    }
    fun storeMessage(chatId: String, senderId: String, message: Message): String {
        val messageId = UUID.randomUUID().toString()
        transaction {
            Messages.insert {
                it[Messages.messageId] = messageId
                it[Messages.chatId] = chatId
                it[Messages.senderId] = senderId
                it[Messages.contentType] = message.contentType
                it[Messages.content] = message.content
                it[Messages.createdAt] = System.currentTimeMillis()
            }
        }
        return messageId
    }

    fun markMessageAsRead(messageId: String, userId: String): Boolean {
        return transaction {
            Messages.update({ Messages.messageId eq messageId }) {
                it[Messages.isRead] = true
                it[Messages.readAt] = System.currentTimeMillis()
            } > 0
        }
    }

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

    fun getAllUsers(): List<String> {
        return transaction {
            ProfileTable.slice(ProfileTable.userId).selectAll().map { it[ProfileTable.userId] }
        }
    }


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
    fun notifyParticipants(chatId: String, message: Message, senderId: String) {
        val participants = getParticipants(chatId).filter { it != senderId } // Exclude sender
        participants.forEach { userId ->
            val deviceToken = profileRepository.getDeviceToken (userId) // Use ProfileRepository method
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

}
