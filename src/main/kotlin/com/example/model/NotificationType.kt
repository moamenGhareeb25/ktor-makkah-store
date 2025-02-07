package com.example.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType(val value: String) {
    @SerialName("task") TASK("task"),
    @SerialName("update") UPDATE("update"),
    @SerialName("chat") CHAT("chat"),
    @SerialName("notification") NOTIFICATION("notification"),
    @SerialName("delegation") DELEGATION("delegation"),
    @SerialName("profile_request") REQUEST("profile_request"); // ✅ Fixed name and syntax

    companion object {
        fun fromValue(value: String): NotificationType? {
            return entries.find { it.value == value }
        }
    }
}

