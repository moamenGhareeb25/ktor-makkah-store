package com.example.database

import org.jetbrains.exposed.sql.Table


object ProfileTable : Table("profiles") {
    val userId = varchar("user_id", 50)
    val name = varchar("name", 100).default("").nullable()
    val email = varchar("email", 100).nullable() // Nullable
    val nickname = varchar("nickname", 50).nullable()
    val personalNumber = varchar("personal_number", 14).nullable()
    val workNumber = varchar("work_number", 14).nullable()
    val profilePictureUrl = varchar("profile_picture_url", 200).nullable()
    val createdAt = long("created_at").nullable()
    val isOnline = bool("is_online").default(false)
    val lastSeen = long("last_seen").nullable()
    val userRole = varchar("user_role", 20).nullable()
    val pendingUpdates = text("pending_updates").nullable()



    override val primaryKey = PrimaryKey(userId)
}

