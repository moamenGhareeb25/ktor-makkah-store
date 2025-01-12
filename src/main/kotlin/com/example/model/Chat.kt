package com.example.model

@kotlinx.serialization.Serializable
data class Chat(
    val chatId: String,
    val chatType: String,
    val createdAt: Long,
    val groupName: String? = null
)