package com.example.dataFactory

import com.example.database.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object DatabaseFactory {
    fun init() {
        try {
            // 🔹 Get the full NeonDB connection string from one env variable
            val databaseUrl = System.getenv("DATABASE_NEON_URL")
                ?: throw IllegalStateException("❌ DATABASE_NEON_URL not set!")

            // 🔹 Parse the URL to extract user, password, host, and database name
            val uri = URI(databaseUrl)
            val userInfo = uri.userInfo.split(":")
            val databaseUser = userInfo[0]
            val databasePassword = userInfo[1]
            val jdbcUrl = "jdbc:postgresql://${uri.host}${uri.path}?sslmode=require"

            Database.connect(
                url = jdbcUrl,
                driver = "org.postgresql.Driver",
                user = databaseUser,
                password = databasePassword
            )

            // 🔹 Use createMissingTablesAndColumns() to avoid overwriting tables
            transaction {
                SchemaUtils.createMissingTablesAndColumns(
                    ProfileTable,
                    Chats,
                    ChatParticipants,
                    Messages,
                    DeviceTokens,
                    Tasks
                )
            }

            println("✅ Database connected successfully!")
        } catch (e: Exception) {
            println("❌ Error initializing database: ${e.message}")
            e.printStackTrace()
        }
    }
}
