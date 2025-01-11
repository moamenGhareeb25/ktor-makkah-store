package com.example.database

import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        try {
            val env = dotenv {
                directory = "./"
                filename = ".env"
            }

            val databaseUrl = env["DATABASE_URL"]
                ?: throw IllegalStateException("DATABASE_URL not set")
            val databaseUser = env["DATABASE_USER"]
                ?: throw IllegalStateException("DATABASE_USER not set")
            val databasePassword = env["DATABASE_PASSWORD"]
                ?: throw IllegalStateException("DATABASE_PASSWORD not set")

            println("Database URL: [REDACTED]") // Avoid logging sensitive data
            println("Database User: [REDACTED]")

            Database.connect(
                url = databaseUrl,
                user = databaseUser,
                password = databasePassword,
                driver = "org.postgresql.Driver"
            )
            println("Database initialized successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error initializing database: ${e.message}")
        }
    }
}