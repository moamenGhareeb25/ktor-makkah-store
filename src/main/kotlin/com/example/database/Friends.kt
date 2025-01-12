package com.example.database

import org.jetbrains.exposed.sql.Table

object Friends : Table("friends") {
    val userId = varchar("user_id", 50)
    val friendId = varchar("friend_id", 50)
}
