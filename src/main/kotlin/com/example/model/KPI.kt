package com.example.model


import kotlinx.serialization.Serializable

@Serializable
data class KPI(
    val kpiId: Int? = null,
    val userId: String,
    val taskId: Int? = null,
    val category: String,
    val value: Int,
    val createdBy: String,
    val createdAt: Long? = null
)
