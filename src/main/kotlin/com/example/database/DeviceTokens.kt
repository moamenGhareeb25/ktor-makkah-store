package com.example.database

import org.jetbrains.exposed.sql.Table



object DeviceTokens : Table("device_tokens") {
    val userId = varchar("user_id", 50).references(ProfileTable.userId)
    val token = varchar("token", 200)

    override val primaryKey = PrimaryKey(userId, token)
}
