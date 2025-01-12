package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class StatusUpdate(
    val userId: String,
    val isOnline: Boolean
)