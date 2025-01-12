package com.example.model

import kotlinx.serialization.Serializable


@Serializable
data class TypingIndicator(
    val chatId: String,
    val userId: String,
    val isTyping: Boolean
)