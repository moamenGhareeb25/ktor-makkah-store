package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class UpdateKey {
    NAME,
    EMAIL,
    NICKNAME,
    PERSONAL_NUMBER,
    WORK_NUMBER,
    PROFILE_PICTURE_URL,
    USER_ROLE
}
