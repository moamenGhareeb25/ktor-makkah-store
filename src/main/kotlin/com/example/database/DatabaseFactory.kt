package com.example.database

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        try {
            val databaseUrl = System.getenv("DATABASE_URL")
                ?: "postgresql://ktor_dkwc_user:xPnzorWy9NzjuPeSYSOFOh7fsiSQ9q7f@dpg-ctvj1qdds78s73em9e1g-a.oregon-postgres.render.com/ktor_dkwc"
            val databaseUser = System.getenv("DATABASE_USER") ?: "ktor_dkwc_user"
            val databasePassword = System.getenv("DATABASE_PASSWORD") ?: "xPnzorWy9NzjuPeSYSOFOh7fsiSQ9q7f"

            println("Database URL: $databaseUrl")
            println("Database User: $databaseUser")

            Database.connect(
                url = databaseUrl,
                user = databaseUser,
                password = databasePassword,
                driver = "org.postgresql.Driver" // Ensure the driver is specified
            )
            println("Database initialized successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error initializing database: ${e.message}")
        }
    }
}