package com.example.database

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        try {
            // Read environment variables directly
            val databaseUrl = System.getenv("DATABASE_URL")
                ?: throw IllegalStateException("DATABASE_URL not set")
            val databaseUser = System.getenv("DATABASE_USER")
                ?: throw IllegalStateException("DATABASE_USER not set")
            val databasePassword = System.getenv("DATABASE_PASSWORD")
                ?: throw IllegalStateException("DATABASE_PASSWORD not set")

            println("Database URL: [REDACTED]") // Avoid logging sensitive data
            println("Database User: [REDACTED]")

            // Connect to the database
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