package com.example.database

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        try {
            val databaseUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/ktor_dkwc"
            val databaseUser = System.getenv("DATABASE_USER") ?: "ktor_dkwc_user"
            val databasePassword = System.getenv("DATABASE_PASSWORD") ?: "xPnzorWy9NzjuPeSYSOFOh7fsiSQ9q7f"

            println("Database URL: $databaseUrl")
            println("Database User: $databaseUser")

            Database.connect(
                url = databaseUrl,
                user = databaseUser,
                password = databasePassword
            )
            println("Database initialized successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error initializing database: ${e.message}")
        }
    }
}