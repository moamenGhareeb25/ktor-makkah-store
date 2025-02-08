package com.example.dataFactory

import com.example.database.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object DatabaseFactory {
    fun init() {
        try {
            // 🔹 Get database URL from the environment
            val databaseUrl = System.getenv("DATABASE_NEON_URL")
                ?: throw IllegalStateException("❌ DATABASE_NEON_URL not set!")

            // 🔹 Ensure proper parsing
            val regex = Regex("jdbc:postgresql://([^:]+):([^@]+)@([^/]+)/([^?]+)")
            val matchResult = regex.find(databaseUrl)
                ?: throw IllegalStateException("❌ Invalid DATABASE_NEON_URL format!")

            val (databaseUser, databasePassword, databaseHost, databaseName) = matchResult.destructured
            val jdbcUrl = "jdbc:postgresql://$databaseHost/$databaseName?sslmode=require"

            // 🔹 Connect to Database
            Database.connect(
                url = jdbcUrl,
                driver = "org.postgresql.Driver",
                user = databaseUser,
                password = databasePassword
            )

            // 🔹 Create Tables
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
