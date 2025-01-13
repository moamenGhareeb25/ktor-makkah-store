package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val userId: String,
    val name: String,
    val email: String,
    val personalNumber: String?,
    val workNumber: String?,
    val profilePictureUrl: String?,
    val userRule: String?,
    val createdAt: Long? = null,
    val pendingUpdates: MutableMap<UpdateKey, String?> = mutableMapOf() // Map with enum keys and nullable String values
) {
    init {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(name.isNotBlank()) { "Name must not be blank" }
        require(email.isNotBlank()) { "Email must not be blank" }
    }
}

