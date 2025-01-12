package com.example.model

@kotlinx.serialization.Serializable
data class Message(
    val senderId: String,
    val contentType: String,
    val content: String
)