package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateChatRequest(
    val chatType: String,
    val participants: List<String>
)