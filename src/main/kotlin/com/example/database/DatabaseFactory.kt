package com.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://dpg-ctvj1qdds78s73em9e1g-a.oregon-postgres.render.com:5432/ktor_dkwc"
            username = System.getenv("DATABASE_USER") ?: "ktor_dkwc_user"
            password = System.getenv("DATABASE_PASSWORD") ?: "xPnzorWy9NzjuPeSYSOFOh7fsiSQ9q7f"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
    }
}