package com.example.database

import org.jetbrains.exposed.sql.Table


object ProfileTable : Table("profiles") {
    val userId = varchar("user_id", 50)
    val name = varchar("name", 100)
    val email = varchar("email", 100)
    val personalNumber = varchar("personal_number", 20).nullable()
    val workNumber = varchar("work_number", 20).nullable()
    val profilePictureUrl = varchar("profile_picture_url", 200).nullable()

    override val primaryKey = PrimaryKey(userId)
}