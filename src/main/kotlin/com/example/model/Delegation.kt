package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Delegation(
    val delegationId: Int? = null,
    val managerId: String,
    val role: String, // "ProfileReviewer" or "KPIUpdater"
    val assignedBy: String,
    val assignedAt: Long? = null
)
