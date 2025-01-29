package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val userId: String,
    val name: String?, // Nullable String
    val email: String?, // Nullable String
    val personalNumber: String?,
    val workNumber: String?,
    val profilePictureUrl: String?,
    val userRole: String?,
    val createdAt: Long? = null,
    val nickname: String?,
    val isOnline: Boolean,
    val lastSeen: Long?,
    val pendingUpdates: MutableMap<UpdateKey, String?> = mutableMapOf()
) {
    init {
        require(userId.isNotBlank()) { "User ID must not be blank" }

        // Null check for name
        require(!name.isNullOrBlank()) { "Name must not be null or blank" }

        // Null check for email
        require(!email.isNullOrBlank()) { "Email must not be null or blank" }
    }
}


