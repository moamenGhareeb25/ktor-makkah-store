package com.example.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        try {
            val databaseUrl = System.getenv("DATABASE_URL")
                ?: throw IllegalStateException("DATABASE_URL not set")
            val databaseUser = System.getenv("DATABASE_USER")
                ?: throw IllegalStateException("DATABASE_USER not set")
            val databasePassword = System.getenv("DATABASE_PASSWORD")
                ?: throw IllegalStateException("DATABASE_PASSWORD not set")

            Database.connect(
                url = databaseUrl,
                user = databaseUser,
                password = databasePassword,
                driver = "org.postgresql.Driver"
            )

            // Create the profiles table if it doesn't exist
            transaction {
                SchemaUtils.create(ProfileTable)
            }

            println("Database initialized successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error initializing database: ${e.message}")
        }
    }
}