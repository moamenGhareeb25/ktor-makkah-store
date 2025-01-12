package com.example.model

@kotlinx.serialization.Serializable
data class CreateGroupChatRequest(
    val adminId: String,
    val groupName: String,
    val participants: List<String>
)