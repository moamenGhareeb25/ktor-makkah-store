package com.example.database

import org.jetbrains.exposed.sql.Table

object WorkLogs : Table("work_logs") {
    val logId = integer("log_id").autoIncrement()
    val userId = varchar("user_id", 50).references(ProfileTable.userId)
    val startTime = long("start_time")
    val endTime = long("end_time").nullable()
    override val primaryKey = PrimaryKey(logId)
}
