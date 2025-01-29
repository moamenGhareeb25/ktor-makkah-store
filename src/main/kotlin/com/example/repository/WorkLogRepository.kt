package com.example.repository

import com.example.database.WorkLogs
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class WorkLogRepository {

    // Log start of work
    fun startWork(userId: String): Int {
        return transaction {
            WorkLogs.insert {
                it[this.userId] = userId
                it[this.startTime] = System.currentTimeMillis()
            }[WorkLogs.logId]
        }
    }

    // Log end of work
    fun endWork(userId: String): Boolean {
        return transaction {
            WorkLogs.update({ WorkLogs.userId eq userId and (WorkLogs.endTime.isNull()) }) {
                it[this.endTime] = System.currentTimeMillis()
            } > 0
        }
    }

    // Fetch total work hours for a user
    fun getTotalWorkHours(userId: String): Long {
        return transaction {
            WorkLogs.select { WorkLogs.userId eq userId }
                .mapNotNull {
                    val start = it[WorkLogs.startTime]
                    val end = it[WorkLogs.endTime] ?: System.currentTimeMillis()
                    end - start
                }.sum()
        }
    }
}
