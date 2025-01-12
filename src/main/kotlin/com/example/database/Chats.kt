package com.example.database

import org.jetbrains.exposed.sql.Table

object Chats : Table("chats") {
    val chatId = varchar("chat_id", 50)
    val chatType = varchar("chat_type", 10)
    val createdAt = long("created_at")
    val groupName = varchar("group_name", 100).nullable()

    override val primaryKey = PrimaryKey(chatId)
}