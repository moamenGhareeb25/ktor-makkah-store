package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val userId: String,
    val name: String,
    val email: String,
    val personalNumber: String?,
    val workNumber: String?,
    val profilePictureUrl: String?
)