package com.example.database

import org.jetbrains.exposed.sql.Table

object ChatParticipants : Table("chat_participants") {
    val chatId = varchar("chat_id", 50).references(Chats.chatId)
    val userId = varchar("user_id", 50).references(ProfileTable.userId)
    val joinedAt = long("joined_at")

    override val primaryKey = PrimaryKey(chatId, userId)
}