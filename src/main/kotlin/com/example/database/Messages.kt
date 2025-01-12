package com.example.database

import org.jetbrains.exposed.sql.Table

object Messages : Table("messages") {
    val messageId = varchar("message_id", 50)
    val chatId = varchar("chat_id", 50).references(Chats.chatId)
    val senderId = varchar("sender_id", 50).references(ProfileTable.userId)
    val contentType = varchar("content_type", 10)
    val content = text("content")
    val createdAt = long("created_at")
    val isRead = bool("is_read").default(false)
    val readAt = long("read_at").nullable()

    override val primaryKey = PrimaryKey(messageId)
}

