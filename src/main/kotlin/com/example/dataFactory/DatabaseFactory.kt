package com.example.dataFactory

import com.example.database.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import io.github.cdimascio.dotenv.dotenv


object DatabaseFactory {
    fun init() {
        try {
            val dotenv = dotenv()
            val databaseUrl = dotenv["DATABASE_URL_neon"]
                ?: throw IllegalStateException("DATABASE_URL_neon not set")
            val databaseUser = dotenv["DATABASE_USER_neon"]
                ?: throw IllegalStateException("DATABASE_USER_neon not set")
            val databasePassword = dotenv["DATABASE_PASSWORD_neon"]
                ?: throw IllegalStateException("DATABASE_PASSWORD_neon not set")

            Database.connect(
                url = databaseUrl,
                user = databaseUser,
                password = databasePassword,
                driver = "org.postgresql.Driver"
            )

            // Create all tables
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

            println("Database initialized successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error initializing database: ${e.message}")
        }
    }
}