package com.example.model

data class Notification(
    val profileId: String,
    val message: String,
    val recipientRole: String,
    val changes: Map<String, Any?>, // Proposed changes
    var status: String = "PENDING" // PENDING, ACCEPTED, REJECTED
)

