package com.example.model

import com.example.database.ProfileTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Column

@Serializable
enum class UpdateKey {
    NAME,
    EMAIL,
    NICKNAME,
    PERSONAL_NUMBER,
    WORK_NUMBER,
    PROFILE_PICTURE_URL,
    USER_ROLE;

    /**
     * Maps UpdateKey to the corresponding ProfileTable column.
     */
    fun getColumn(): Column<String?> {
        return when (this) {
            NAME -> ProfileTable.name
            EMAIL -> ProfileTable.email
            NICKNAME -> ProfileTable.nickname
            PERSONAL_NUMBER -> ProfileTable.personalNumber
            WORK_NUMBER -> ProfileTable.workNumber
            PROFILE_PICTURE_URL -> ProfileTable.profilePictureUrl
            USER_ROLE -> ProfileTable.userRole
        }
    }
}

