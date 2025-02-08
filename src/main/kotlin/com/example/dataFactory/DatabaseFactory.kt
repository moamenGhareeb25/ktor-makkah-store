package com.example.dataFactory

import com.example.database.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object DatabaseFactory {
    fun init() {
        try {
            // 🔹 Get the database URL from environment
            val databaseUrl = System.getenv("DATABASE_NEON_URL")
                ?: throw IllegalStateException("❌ DATABASE_NEON_URL not set!")

            println("🔍 DATABASE_NEON_URL: $databaseUrl")  // Print full connection URL

            // 🔹 Parse the URL to extract credentials
            val uri = URI(databaseUrl)
            val userInfo = uri.userInfo ?: throw IllegalStateException("❌ User info missing in DB URL!")
            val userParts = userInfo.split(":")
            if (userParts.size < 2) throw IllegalStateException("❌ Invalid user info format in DB URL!")

            val databaseUser = userParts[0]
            val databasePassword = userParts[1]
            val jdbcUrl = "jdbc:postgresql://${uri.host}${uri.path}?sslmode=require"

            println("🔹 Parsed DB Credentials:")
            println("   - User: $databaseUser")
            println("   - Password: ${"*".repeat(databasePassword.length)}")
            println("   - JDBC URL: $jdbcUrl")

            // 🔹 Connect to the database
            Database.connect(
                url = jdbcUrl,
                driver = "org.postgresql.Driver",
                user = databaseUser,
                password = databasePassword
            )

            // 🔹 Create tables if missing
            transaction {
                SchemaUtils.create(
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
