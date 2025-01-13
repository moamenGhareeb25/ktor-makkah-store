package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val taskId: Int? = null,
    val title: String,
    val description: String? = null,
    val status: String = "pending",
    val dueDate: Long? = null,
    val assignedTo: String? = null,
    val createdBy: String,
    val createdAt: Long? = null
)
